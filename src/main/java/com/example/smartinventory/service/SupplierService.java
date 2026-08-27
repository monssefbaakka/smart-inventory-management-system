package com.example.smartinventory.service;

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
     * @param id identifier of the supplier
     * @return that supplier's record over the deliveries that can be judged
     * @throws ResourceNotFoundException if the supplier does not exist
     */
    @Transactional(readOnly = true)
    public SupplierReliabilityResponse reliability(Long id) {
        Supplier supplier = findById(id);
        return SupplierReliabilityResponse.of(supplier,
                purchaseOrderRepository.findJudgeableDeliveries(id, PurchaseOrderStatus.RECEIVED));
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
     * @return every supplier's record, the worst of them first
     */
    @Transactional(readOnly = true)
    public List<SupplierReliabilityResponse> reliability() {
        Map<Long, List<PurchaseOrder>> deliveriesBySupplier = purchaseOrderRepository
                .findJudgeableDeliveries(PurchaseOrderStatus.RECEIVED).stream()
                .collect(Collectors.groupingBy(order -> order.getSupplier().getId()));

        return supplierRepository.findAll().stream()
                .map(supplier -> SupplierReliabilityResponse.of(supplier,
                        deliveriesBySupplier.getOrDefault(supplier.getId(), List.of())))
                .sorted(WORST_FIRST)
                .toList();
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
