package com.example.smartinventory.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.SupplierReliabilityResponse;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.model.PurchaseOrderStatus;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.PurchaseOrderRepository;
import com.example.smartinventory.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

/** Service exposing CRUD operations for {@link Supplier}. */
@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    /**
     * The order the league table is read in: the worst proportion on time first, the suppliers with
     * no record at all last, ties settled by how many orders each was judged on and then by name.
     */
    private static final Comparator<SupplierReliabilityResponse> WORST_FIRST = Comparator
            .comparing(SupplierReliabilityResponse::onTimeRate,
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Comparator.comparingLong(SupplierReliabilityResponse::ordersJudged).reversed())
            .thenComparing(SupplierReliabilityResponse::supplierName,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    private final SupplierRepository supplierRepository;
    private final WarehouseService warehouseService;
    private final PurchaseOrderRepository purchaseOrderRepository;

    /**
     * Persists a new supplier, resolving the warehouse its goods are normally delivered to.
     *
     * @param supplier the supplier to create
     * @return the persisted supplier
     * @throws ResourceNotFoundException if the named default warehouse does not exist
     */
    public Supplier create(Supplier supplier) {
        supplier.setDefaultWarehouse(resolveDefaultWarehouse(supplier.getDefaultWarehouse()));
        return supplierRepository.save(supplier);
    }

    @Transactional(readOnly = true)
    public Supplier findById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        return supplierRepository.findAll();
    }

    /**
     * Updates the mutable fields of an existing supplier identified by {@code id}.
     *
     * <p>Changing where the supplier's goods are normally delivered, or how long they take to arrive,
     * applies to the orders raised from here on. Orders already raised recorded the warehouse they
     * were given, and orders already placed recorded the date they were due: where a delivery is
     * going and when it is expected were both settled when the order went out to the supplier.
     *
     * @param id              identifier of the supplier to update
     * @param updatedSupplier supplier carrying the new field values
     * @return the persisted, updated supplier
     * @throws ResourceNotFoundException if the supplier or the named default warehouse does not exist
     */
    public Supplier update(Long id, Supplier updatedSupplier) {
        Supplier existing = findById(id);
        existing.setName(updatedSupplier.getName());
        existing.setContactName(updatedSupplier.getContactName());
        existing.setEmail(updatedSupplier.getEmail());
        existing.setPhone(updatedSupplier.getPhone());
        existing.setAddress(updatedSupplier.getAddress());
        existing.setDefaultWarehouse(resolveDefaultWarehouse(updatedSupplier.getDefaultWarehouse()));
        existing.setLeadTimeDays(updatedSupplier.getLeadTimeDays());
        return supplierRepository.save(existing);
    }

    public void delete(Long id) {
        supplierRepository.delete(findById(id));
    }

    /**
     * Reports how well a supplier has kept the dates their orders were promised for, reading the
     * expected delivery date and the day the goods arrived back together.
     *
     * <p>Only their fulfilled orders carrying both dates are judged. What a lead time of fourteen
     * days is worth is a question about deliveries that happened, and one order is an anecdote: a
     * supplier who was a week late once is not the supplier who is a week late every time, which is
     * the distinction a buyer placing the next order needs and cannot make one order at a time.
     *
     * <p>What those same orders were worth is reported beside the rates, because a record says how
     * often a supplier is late and not whether being late costs anything: a supplier late on all
     * three of their orders and one late on a third of two hundred read alike until the money is on
     * the row.
     *
     * <p>The record can be taken over the deliveries that arrived on or after a given day. A supplier
     * who was late all last year and has been on time since reads as unreliable over their whole
     * book, and one who has started slipping is covered by a long good history; the buyer placing
     * the next order is asking about the supplier now. Naming no day asks about the whole book,
     * which is the older question and still a fair one.
     *
     * <p>A window catching no delivery reports nothing judged rather than nothing found: over that
     * period the supplier has no record, which is what a supplier nobody has received from has, and
     * the counts, rates and sums say so on the same terms.
     *
     * <p>What the supplier still owes past the day they promised it is reported beside what they
     * delivered. Read as of today and never narrowed by the window: what is outstanding is a fact
     * about now, and a window over days of arrival cannot select an order that has not arrived.
     *
     * @param id    identifier of the supplier
     * @param since the earliest day of arrival to judge, inclusive, or null for their whole record
     * @return that supplier's record over the deliveries that can be judged
     * @throws ResourceNotFoundException if the supplier does not exist
     */
    @Transactional(readOnly = true)
    public SupplierReliabilityResponse reliability(Long id, LocalDate since) {
        Supplier supplier = findById(id);
        List<PurchaseOrder> deliveries = since == null
                ? purchaseOrderRepository.findJudgeableDeliveries(id, PurchaseOrderStatus.RECEIVED)
                : purchaseOrderRepository.findJudgeableDeliveriesSince(id, PurchaseOrderStatus.RECEIVED, since);
        LocalDate today = LocalDate.now();
        List<PurchaseOrder> outstanding = purchaseOrderRepository.findOutstandingPastPromise(id,
                PurchaseOrderStatus.awaitingDelivery(), today);
        return SupplierReliabilityResponse.of(supplier, deliveries, outstanding, today);
    }

    /**
     * Reports every supplier's record of keeping their dates in one table, worst first.
     *
     * <p>Ranked by the proportion of deliveries that arrived on time, ascending, so the suppliers
     * holding the warehouse up are at the top where the question is asked. Suppliers with nothing
     * judged sort last: no record is not a bad record. Ties are settled by the number of orders
     * judged, most first, and then by name, so the table comes back in the same order twice.
     *
     * <p>Every supplier appears, including the ones nobody has received from. Leaving them out would
     * make absence from the table mean two different things — no record, or no supplier — and a
     * buyer wondering who they have never bought from would have no way to tell.
     *
     * <p>The ranking does not weigh confidence: a supplier late on their only delivery sorts above
     * one late on thirty of fifty. {@code ordersJudged} is in every row to say which is which, and a
     * weighting would hide the thin records rather than show them.
     *
     * <p>Nor does it rank on the money. What each row is worth is reported so the expensive problem
     * can be told from the loud one, but a table ordered by spend answers a different question than
     * the one this table answers.
     *
     * <p>The table can be taken over a window of arrivals, on the same terms as one supplier's record.
     * Every supplier still appears: one whose deliveries all fall outside the window has no record
     * over it, which is a thing worth seeing on the table rather than a reason to drop the row.
     *
     * <p>Every row also carries what that supplier still owes past the day they promised it, read as
     * of today. The ranking does not read it: the table is ordered on how deliveries went, and an
     * order that has not arrived has not gone either way. A supplier with nothing judged and a pile
     * outstanding still sorts last, where no record puts them, with the pile on their row saying
     * what the rates cannot.
     *
     * @param since the earliest day of arrival to judge, inclusive, or null for the whole record
     * @return every supplier's record, the worst of them first
     */
    @Transactional(readOnly = true)
    public List<SupplierReliabilityResponse> reliability(LocalDate since) {
        List<PurchaseOrder> deliveries = since == null
                ? purchaseOrderRepository.findJudgeableDeliveries(PurchaseOrderStatus.RECEIVED)
                : purchaseOrderRepository.findJudgeableDeliveriesSince(PurchaseOrderStatus.RECEIVED, since);
        Map<Long, List<PurchaseOrder>> deliveriesBySupplier = bySupplier(deliveries);

        LocalDate today = LocalDate.now();
        Map<Long, List<PurchaseOrder>> outstandingBySupplier = bySupplier(purchaseOrderRepository
                .findOutstandingPastPromise(PurchaseOrderStatus.awaitingDelivery(), today));

        return supplierRepository.findAll().stream()
                .map(supplier -> SupplierReliabilityResponse.of(supplier,
                        deliveriesBySupplier.getOrDefault(supplier.getId(), List.of()),
                        outstandingBySupplier.getOrDefault(supplier.getId(), List.of()), today))
                .sorted(WORST_FIRST)
                .toList();
    }

    /**
     * Groups orders by the supplier they were raised against.
     *
     * @param orders the orders to group
     * @return the orders under their supplier's identifier
     */
    private static Map<Long, List<PurchaseOrder>> bySupplier(List<PurchaseOrder> orders) {
        return orders.stream().collect(Collectors.groupingBy(order -> order.getSupplier().getId()));
    }

    /**
     * Replaces the warehouse carried by a request payload with the persisted one it names, so the
     * supplier holds a managed reference whose code can be rendered into the response. A payload
     * naming no warehouse, or naming one without an id, leaves the supplier with no usual
     * destination.
     *
     * @param requested the warehouse as sent by the caller, possibly {@code null}
     * @return the persisted warehouse, or {@code null} when none was named
     * @throws ResourceNotFoundException if the named warehouse does not exist
     */
    private Warehouse resolveDefaultWarehouse(Warehouse requested) {
        if (requested == null || requested.getId() == null) {
            return null;
        }
        return warehouseService.findById(requested.getId());
    }

}
