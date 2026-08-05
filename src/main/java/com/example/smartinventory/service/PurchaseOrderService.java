package com.example.smartinventory.service;

import java.util.LinkedHashMap;
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
import com.example.smartinventory.exception.InvalidPurchaseOrderStateException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.model.PurchaseOrderItem;
import com.example.smartinventory.model.PurchaseOrderStatus;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.repository.PurchaseOrderRepository;

import lombok.RequiredArgsConstructor;

/** Service managing purchase orders and their lifecycle transitions. */
@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final StockMovementService stockMovementService;

    /**
     * Creates a new purchase order in {@code DRAFT} status for the given supplier, resolving
     * each requested product into a line item.
     *
     * @param request the supplier, optional note, and line items to order
     * @return the persisted draft order
     */
    public PurchaseOrder create(PurchaseOrderRequest request) {
        Supplier supplier = supplierService.findById(request.supplierId());

        PurchaseOrder order = PurchaseOrder.builder()
                .supplier(supplier)
                .status(PurchaseOrderStatus.DRAFT)
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
     * Returns one page of purchase orders, optionally narrowed to a single supplier.
     *
     * <p>The orders are mapped to their responses inside this transaction: the paged query fetches
     * the supplier but deliberately not the line items, because fetching a collection alongside a
     * page would force the page to be assembled in memory. The items of the orders on the page are
     * loaded here instead, while the session is still open.
     *
     * @param supplierId identifier of a supplier to filter by, or {@code null}
     * @param pageable   the page to return and the order to return it in
     * @return the requested page of matching orders
     * @throws ResourceNotFoundException if the named supplier does not exist
     */
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> find(Long supplierId, Pageable pageable) {
        Page<PurchaseOrder> orders;
        if (supplierId == null) {
            orders = purchaseOrderRepository.findAllBy(pageable);
        } else {
            supplierService.findById(supplierId);
            orders = purchaseOrderRepository.findBySupplierId(supplierId, pageable);
        }
        return orders.map(PurchaseOrderResponse::from);
    }

    /**
     * Transitions a {@code DRAFT} order to {@code PLACED}.
     *
     * @param id identifier of the order
     * @return the placed order
     * @throws InvalidPurchaseOrderStateException if the order is not in {@code DRAFT}
     */
    public PurchaseOrder place(Long id) {
        PurchaseOrder order = findById(id);
        requireStatus(order, PurchaseOrderStatus.DRAFT, "placed");
        order.setStatus(PurchaseOrderStatus.PLACED);
        return purchaseOrderRepository.save(order);
    }

    /**
     * Receives everything still outstanding on an order, leaving it {@code RECEIVED}.
     *
     * <p>This is the whole-delivery shorthand for {@link #receive(Long, GoodsReceiptRequest)}: it
     * books the outstanding quantity of every line, which for an order nothing has arrived against
     * is the full quantity ordered. An order already part-delivered is closed out by the same call.
     *
     * @param id identifier of the order
     * @return the received order
     * @throws InvalidPurchaseOrderStateException if the order is not awaiting delivery
     */
    public PurchaseOrder receive(Long id) {
        PurchaseOrder order = findById(id);
        requireAwaitingDelivery(order);

        for (PurchaseOrderItem item : order.getItems()) {
            book(order, item, item.getOutstandingQuantity());
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
     * <p>A delivery is one transaction: if any line asks for more than it has outstanding, nothing
     * at all is booked.
     *
     * @param id      identifier of the order
     * @param request the lines that arrived and how much of each
     * @return the order as the delivery left it
     * @throws InvalidPurchaseOrderStateException if the order is not awaiting delivery, or a line
     *                                            would be received past the quantity ordered
     * @throws ResourceNotFoundException          if a named line is not on this order
     */
    public PurchaseOrder receive(Long id, GoodsReceiptRequest request) {
        PurchaseOrder order = findById(id);
        requireAwaitingDelivery(order);

        Map<Long, PurchaseOrderItem> itemsById = order.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, item -> item));

        Map<Long, Integer> delivered = new LinkedHashMap<>();
        for (GoodsReceiptLineRequest line : request.lines()) {
            if (!itemsById.containsKey(line.itemId())) {
                throw new ResourceNotFoundException(
                        "Line item " + line.itemId() + " is not on purchase order " + id);
            }
            delivered.merge(line.itemId(), line.quantity(), Integer::sum);
        }

        delivered.forEach((itemId, quantity) -> {
            PurchaseOrderItem item = itemsById.get(itemId);
            if (quantity > item.getOutstandingQuantity()) {
                throw new InvalidPurchaseOrderStateException(
                        "Cannot receive " + quantity + " units against line item " + itemId + " of purchase order "
                                + id + ": only " + item.getOutstandingQuantity() + " outstanding of "
                                + item.getQuantity() + " ordered");
            }
        });

        delivered.forEach((itemId, quantity) -> book(order, itemsById.get(itemId), quantity));

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
     * @param order    the order being delivered against
     * @param item     the line the goods belong to
     * @param quantity units that arrived
     */
    private void book(PurchaseOrder order, PurchaseOrderItem item, int quantity) {
        if (quantity <= 0) {
            return;
        }
        stockMovementService.record(item.getProduct().getId(), null, null, MovementType.IN, quantity,
                "Purchase order #" + order.getId() + " received", item.getUnitPrice());
        item.setReceivedQuantity(item.getReceivedQuantity() + quantity);
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

}
