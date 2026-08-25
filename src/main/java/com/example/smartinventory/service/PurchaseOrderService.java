package com.example.smartinventory.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.GoodsReceiptLineRequest;
import com.example.smartinventory.dto.GoodsReceiptRequest;
import com.example.smartinventory.dto.PurchaseOrderItemRequest;
import com.example.smartinventory.dto.PurchaseOrderRequest;
import com.example.smartinventory.dto.PurchaseOrderResponse;
import com.example.smartinventory.exception.InvalidBatchException;
import com.example.smartinventory.exception.InvalidBatchStateException;
import com.example.smartinventory.exception.InvalidPurchaseOrderStateException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.model.PurchaseOrderItem;
import com.example.smartinventory.model.PurchaseOrderStatus;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.PurchaseOrderRepository;

import lombok.RequiredArgsConstructor;

/** Service managing purchase orders and their lifecycle transitions. */
@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

    /**
     * The statuses an order is still waiting on the supplier in, taken from the states themselves so
     * that what counts as waiting is stated once.
     */
    private static final List<PurchaseOrderStatus> AWAITING_DELIVERY = Arrays.stream(PurchaseOrderStatus.values())
            .filter(PurchaseOrderStatus::isAwaitingDelivery)
            .toList();

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierService supplierService;
    private final WarehouseService warehouseService;
    private final ProductService productService;
    private final StockMovementService stockMovementService;
    private final ProductBatchService productBatchService;

    /**
     * Creates a new purchase order in {@code DRAFT} status for the given supplier, resolving
     * each requested product into a line item.
     *
     * <p>The order may name the warehouse it is to be delivered to, which is what the buyer already
     * knows and the receiving clerk would otherwise have to repeat on every delivery against it. A
     * warehouse no site carries is refused here, when the order is raised, rather than weeks later
     * when the goods turn up against it.
     *
     * <p>An order naming none goes where the supplier's goods normally go, which is a fact about
     * the trading relationship rather than a decision taken order by order. It is read once, here:
     * the order records the warehouse it was given, so moving a supplier's usual destination later
     * does not move the deliveries of orders already out with it.
     *
     * <p>The order may also name when its goods are due, which a buyer who has spoken to the supplier
     * knows better than any average does. An order naming no date is given one from the supplier's
     * lead time when it is {@link #place(Long) placed}, not here: a draft is a document nobody has
     * sent yet, and the days a supplier takes are counted from the day the order reaches them.
     *
     * @param request the supplier, optional delivery warehouse, expected delivery date and note, and
     *                line items to order
     * @return the persisted draft order
     * @throws ResourceNotFoundException if the supplier, a product or the warehouse does not exist
     */
    public PurchaseOrder create(PurchaseOrderRequest request) {
        Supplier supplier = supplierService.findById(request.supplierId());
        Warehouse warehouse = request.warehouseId() == null
                ? supplier.getDefaultWarehouse()
                : warehouseService.findById(request.warehouseId());

        PurchaseOrder order = PurchaseOrder.builder()
                .supplier(supplier)
                .warehouse(warehouse)
                .status(PurchaseOrderStatus.DRAFT)
                .expectedDeliveryDate(request.expectedDeliveryDate())
                .note(request.note())
                .build();

        for (PurchaseOrderItemRequest itemRequest : request.items()) {
            Product product = productService.findById(itemRequest.productId());
            order.addItem(PurchaseOrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.quantity())
                    .unitPrice(itemRequest.unitPrice())
                    .build());
        }

        return purchaseOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public PurchaseOrder findById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));
    }

    /**
     * Returns one page of purchase orders, optionally narrowed to a single supplier and to the ones
     * running late.
     *
     * <p>The orders are mapped to their responses inside this transaction: the paged query fetches
     * the supplier but deliberately not the line items, because fetching a collection alongside a
     * page would force the page to be assembled in memory. The items of the orders on the page are
     * loaded here instead, while the session is still open.
     *
     * <p>Late is judged against today, in the database, on the same terms the order judges itself by
     * when it is read: still awaiting delivery, and due before today. A draft nobody has sent, an
     * order already received or cancelled, and one carrying no expected delivery date are all left
     * out.
     *
     * @param supplierId identifier of a supplier to filter by, or {@code null}
     * @param overdue    when true, keeps only the orders whose goods were due before today
     * @param pageable   the page to return and the order to return it in
     * @return the requested page of matching orders
     * @throws ResourceNotFoundException if the named supplier does not exist
     */
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> find(Long supplierId, boolean overdue, Pageable pageable) {
        if (supplierId != null) {
            supplierService.findById(supplierId);
        }
        return findOrders(supplierId, overdue, pageable).map(PurchaseOrderResponse::from);
    }

    /**
     * Reads the page the filters ask for, each combination having its own finder.
     *
     * @param supplierId identifier of a supplier to filter by, or {@code null}
     * @param overdue    whether to keep only the orders running late
     * @param pageable   the page to return and the order to return it in
     * @return the requested page of orders
     */
    private Page<PurchaseOrder> findOrders(Long supplierId, boolean overdue, Pageable pageable) {
        LocalDate today = LocalDate.now();
        if (supplierId == null) {
            return overdue
                    ? purchaseOrderRepository.findByStatusInAndExpectedDeliveryDateBefore(
                            AWAITING_DELIVERY, today, pageable)
                    : purchaseOrderRepository.findAllBy(pageable);
        }
        return overdue
                ? purchaseOrderRepository.findBySupplierIdAndStatusInAndExpectedDeliveryDateBefore(
                        supplierId, AWAITING_DELIVERY, today, pageable)
                : purchaseOrderRepository.findBySupplierId(supplierId, pageable);
    }

    /**
     * Transitions a {@code DRAFT} order to {@code PLACED}, recording when its goods are due.
     *
     * <p>The date is worked out here rather than when the order was drafted, because a supplier's
     * lead time is counted from the day the order reaches them. A draft raised by the automatic
     * reorder on a Monday and reviewed on the Friday is due four days later than a date stamped at
     * drafting would have promised, and nothing about the supplier changed in between.
     *
     * <p>An order that already names a date keeps it: somebody who has spoken to the supplier about
     * this delivery knows more about it than their usual turnaround does. An order naming none whose
     * supplier names no lead time either is placed without a date, rather than with a guessed one.
     *
     * @param id identifier of the order
     * @return the placed order
     * @throws InvalidPurchaseOrderStateException if the order is not in {@code DRAFT}
     */
    public PurchaseOrder place(Long id) {
        PurchaseOrder order = findById(id);
        requireStatus(order, PurchaseOrderStatus.DRAFT, "placed");
        order.setStatus(PurchaseOrderStatus.PLACED);
        order.setExpectedDeliveryDate(deliveryDateOf(order));
        return purchaseOrderRepository.save(order);
    }

    /**
     * Works out when an order being placed is due: the date it already names, or today plus the lead
     * time of the supplier it is going to.
     *
     * <p>Read once, at placing. The date the order is stamped with is the date it keeps, and a
     * supplier whose lead time is revised afterwards moves the orders raised from then on rather
     * than the ones already out with them.
     *
     * @param order the order being placed
     * @return the date its goods are expected, or {@code null} when nothing is known about it
     */
    private LocalDate deliveryDateOf(PurchaseOrder order) {
        if (order.getExpectedDeliveryDate() != null) {
            return order.getExpectedDeliveryDate();
        }
        Integer leadTimeDays = order.getSupplier().getLeadTimeDays();
        return leadTimeDays == null ? null : LocalDate.now().plusDays(leadTimeDays);
    }

    /**
     * Receives everything still outstanding on an order where the order says it was to be
     * delivered, as {@link #receive(Long, Long)} does.
     *
     * @param id identifier of the order
     * @return the received order
     * @throws InvalidPurchaseOrderStateException if the order is not awaiting delivery
     */
    public PurchaseOrder receive(Long id) {
        return receive(id, (Long) null);
    }

    /**
     * Receives everything still outstanding on an order, leaving it {@code RECEIVED}.
     *
     * <p>This is the whole-delivery shorthand for {@link #receive(Long, GoodsReceiptRequest)}: it
     * books the outstanding quantity of every line, which for an order nothing has arrived against
     * is the full quantity ordered. An order already part-delivered is closed out by the same call.
     *
     * <p>The goods land in one location: the one named here, or else the one the order is to be
     * delivered to, or nowhere in particular when neither says. No lot is named: a shorthand that
     * receives whatever is left cannot know which lot each line arrived as, and that has to be said
     * line by line through {@link #receive(Long, GoodsReceiptRequest)}.
     *
     * @param id          identifier of the order
     * @param warehouseId identifier of the location the goods landed in, overriding the one the
     *                    order is to be delivered to, or {@code null} to use that one
     * @return the received order
     * @throws InvalidPurchaseOrderStateException if the order is not awaiting delivery
     * @throws ResourceNotFoundException          if the named warehouse does not exist
     */
    public PurchaseOrder receive(Long id, Long warehouseId) {
        PurchaseOrder order = findById(id);
        requireAwaitingDelivery(order);

        Long destination = warehouseId == null ? deliveryWarehouseId(order) : warehouseId;
        for (PurchaseOrderItem item : order.getItems()) {
            book(order, item, item.getOutstandingQuantity(), destination, null);
        }

        return settle(order);
    }

    /**
     * Books one delivery against an order: each named line takes the stated quantity into stock as
     * an {@code IN} movement at the line's unit price, so what was paid for the goods rolls into the
     * product's weighted average cost without anyone entering the figure a second time.
     *
     * <p>Lines the request leaves out are not received and stay outstanding. The order ends up
     * {@code RECEIVED} once every line is complete and {@code PARTIALLY_RECEIVED} while anything is
     * still to come.
     *
     * <p>Each part of the delivery is put away where it says it was: the receipt's warehouse unless
     * the line names one of its own, and failing both the warehouse the order is to be delivered
     * to, together with the lot whose code is printed on the goods. A lot code the
     * product does not carry yet starts being tracked here, held in the receiving warehouse, so the
     * stock and the lot it arrived as are one record rather than two. A line may be listed more than
     * once, which is how a delivery split across lots or across sites is expressed; the parts that
     * agree on both are booked together.
     *
     * <p>A delivery is one transaction: if any line asks for more than it has outstanding, counting
     * every part of the delivery that line was split into, nothing at all is booked.
     *
     * @param id      identifier of the order
     * @param request the lines that arrived, how much of each, and where it went
     * @return the order as the delivery left it
     * @throws InvalidPurchaseOrderStateException if the order is not awaiting delivery, or a line
     *                                            would be received past the quantity ordered
     * @throws ResourceNotFoundException          if a named line is not on this order, or a named
     *                                            warehouse does not exist
     * @throws InvalidBatchException              if a line states an expiry date but no lot code
     * @throws InvalidBatchStateException         if a lot code the product carries is stated under a
     *                                            different expiry date
     */
    public PurchaseOrder receive(Long id, GoodsReceiptRequest request) {
        PurchaseOrder order = findById(id);
        requireAwaitingDelivery(order);

        Map<Long, PurchaseOrderItem> itemsById = order.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, item -> item));

        Long receiptWarehouseId = request.warehouseId() == null
                ? deliveryWarehouseId(order)
                : request.warehouseId();

        Map<Destination, Integer> delivered = new LinkedHashMap<>();
        for (GoodsReceiptLineRequest line : request.lines()) {
            if (!itemsById.containsKey(line.itemId())) {
                throw new ResourceNotFoundException(
                        "Line item " + line.itemId() + " is not on purchase order " + id);
            }
            if (line.lotCode() == null && line.expiryDate() != null) {
                throw new InvalidBatchException("Line item " + line.itemId() + " of purchase order " + id
                        + " states an expiry date but no lot code; an expiry date belongs to a lot");
            }
            delivered.merge(Destination.of(line, receiptWarehouseId), line.quantity(), Integer::sum);
        }

        Map<Long, Integer> deliveredPerLine = new LinkedHashMap<>();
        delivered.forEach((destination, quantity) ->
                deliveredPerLine.merge(destination.itemId(), quantity, Integer::sum));

        deliveredPerLine.forEach((itemId, quantity) -> {
            PurchaseOrderItem item = itemsById.get(itemId);
            if (quantity > item.getOutstandingQuantity()) {
                throw new InvalidPurchaseOrderStateException(
                        "Cannot receive " + quantity + " units against line item " + itemId + " of purchase order "
                                + id + ": only " + item.getOutstandingQuantity() + " outstanding of "
                                + item.getQuantity() + " ordered");
            }
        });

        delivered.forEach((destination, quantity) -> {
            PurchaseOrderItem item = itemsById.get(destination.itemId());
            book(order, item, quantity, destination.warehouseId(), batchIdFor(item, destination));
        });

        return settle(order);
    }

    /**
     * Cancels an order that has not been received in full, abandoning whatever is outstanding on it.
     * Stock already received against a part-delivered order stays where it is; a {@code RECEIVED}
     * order has nothing left to abandon and cannot be cancelled.
     *
     * @param id identifier of the order
     * @return the cancelled order
     * @throws InvalidPurchaseOrderStateException if the order is already received or cancelled
     */
    public PurchaseOrder cancel(Long id) {
        PurchaseOrder order = findById(id);
        if (order.getStatus() == PurchaseOrderStatus.RECEIVED
                || order.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new InvalidPurchaseOrderStateException(
                    "Purchase order " + id + " cannot be cancelled from status " + order.getStatus());
        }
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        return purchaseOrderRepository.save(order);
    }

    public void delete(Long id) {
        purchaseOrderRepository.delete(findById(id));
    }

    /**
     * Takes a quantity of one line into stock and credits it to that line. Booking nothing is a
     * no-op rather than an error: a whole-order receipt walks every line, including the ones an
     * earlier delivery already completed.
     *
     * @param order       the order being delivered against
     * @param item        the line the goods belong to
     * @param quantity    units that arrived
     * @param warehouseId identifier of the location they landed in, or {@code null}
     * @param batchId     identifier of the lot they arrived as, or {@code null}
     */
    private void book(PurchaseOrder order, PurchaseOrderItem item, int quantity, Long warehouseId, Long batchId) {
        if (quantity <= 0) {
            return;
        }
        stockMovementService.record(item.getProduct().getId(), warehouseId, batchId, MovementType.IN, quantity,
                "Purchase order #" + order.getId() + " received", item.getUnitPrice());
        item.setReceivedQuantity(item.getReceivedQuantity() + quantity);
    }

    /**
     * Reads off where an order was to be delivered, which is where a receipt against it books
     * unless the receipt or one of its lines says otherwise.
     *
     * @param order the order being delivered against
     * @return identifier of the warehouse the order names, or {@code null} when it names none
     */
    private Long deliveryWarehouseId(PurchaseOrder order) {
        return order.getWarehouse() == null ? null : order.getWarehouse().getId();
    }

    /**
     * Resolves the lot a part of a delivery arrived as, starting to track it when the product does
     * not carry that code yet.
     *
     * @param item        the line the goods belong to
     * @param destination where that part of the delivery was put away
     * @return identifier of the lot, or {@code null} when the goods arrived under no lot code
     */
    private Long batchIdFor(PurchaseOrderItem item, Destination destination) {
        if (destination.lotCode() == null) {
            return null;
        }
        return productBatchService.findOrCreate(item.getProduct().getId(), destination.warehouseId(),
                destination.lotCode(), destination.expiryDate()).getId();
    }

    /**
     * Moves the order to the status its lines now warrant and saves it.
     *
     * @param order the order a delivery was just booked against
     * @return the saved order
     */
    private PurchaseOrder settle(PurchaseOrder order) {
        order.setStatus(order.isFullyReceived()
                ? PurchaseOrderStatus.RECEIVED
                : PurchaseOrderStatus.PARTIALLY_RECEIVED);
        return purchaseOrderRepository.save(order);
    }

    /**
     * Requires that the order is one a delivery may still be booked against.
     *
     * @param order the order to check
     * @throws InvalidPurchaseOrderStateException if it is not placed or part-delivered
     */
    private void requireAwaitingDelivery(PurchaseOrder order) {
        if (order.getStatus() != PurchaseOrderStatus.PLACED
                && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new InvalidPurchaseOrderStateException(
                    "Purchase order " + order.getId() + " cannot be received from status " + order.getStatus()
                            + "; expected " + PurchaseOrderStatus.PLACED + " or "
                            + PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }
    }

    private void requireStatus(PurchaseOrder order, PurchaseOrderStatus expected, String action) {
        if (order.getStatus() != expected) {
            throw new InvalidPurchaseOrderStateException(
                    "Purchase order " + order.getId() + " cannot be " + action + " from status "
                            + order.getStatus() + "; expected " + expected);
        }
    }

    /**
     * Where one part of a delivery was put away: the line it belongs to, the location it landed in
     * and the lot it arrived as. Two parts agreeing on all of it are the same goods in the same
     * place and are booked as one movement, while parts differing anywhere are booked separately —
     * that is what a mixed pallet is.
     *
     * @param itemId      identifier of the purchase-order line the goods belong to
     * @param warehouseId identifier of the location they landed in, or {@code null}
     * @param lotCode     the lot code printed on them, or {@code null}
     * @param expiryDate  the lot's expiry date, or {@code null}
     */
    private record Destination(Long itemId, Long warehouseId, String lotCode, LocalDate expiryDate) {

        /**
         * Reads off where a requested line landed, falling back to the receipt's warehouse for a
         * line that names none of its own.
         *
         * @param line               the requested line
         * @param receiptWarehouseId identifier of the warehouse the whole delivery landed in, or
         *                           {@code null}
         * @return the destination that line is booked to
         */
        static Destination of(GoodsReceiptLineRequest line, Long receiptWarehouseId) {
            return new Destination(line.itemId(),
                    line.warehouseId() == null ? receiptWarehouseId : line.warehouseId(),
                    line.lotCode(),
                    line.expiryDate());
        }
    }

}
