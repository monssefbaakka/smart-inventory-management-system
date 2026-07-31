package com.example.smartinventory.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Transitions a {@code PLACED} order to {@code RECEIVED}, recording an {@code IN} stock
     * movement for each line item so received goods increase product quantities through the
     * shared stock-movement audit trail.
     *
     * @param id identifier of the order
     * @return the received order
     * @throws InvalidPurchaseOrderStateException if the order is not in {@code PLACED}
     */
    public PurchaseOrder receive(Long id) {
        PurchaseOrder order = findById(id);
        requireStatus(order, PurchaseOrderStatus.PLACED, "received");

        for (PurchaseOrderItem item : order.getItems()) {
            stockMovementService.record(item.getProduct().getId(), null, MovementType.IN, item.getQuantity(),
                    "Purchase order #" + order.getId() + " received");
        }

        order.setStatus(PurchaseOrderStatus.RECEIVED);
        return purchaseOrderRepository.save(order);
    }

    /**
     * Cancels a {@code DRAFT} or {@code PLACED} order. A {@code RECEIVED} order cannot be
     * cancelled because its stock effect has already been applied.
     *
     * @param id identifier of the order
     * @return the cancelled order
     * @throws InvalidPurchaseOrderStateException if the order is already received or cancelled
     */
    public PurchaseOrder cancel(Long id) {
        PurchaseOrder order = findById(id);
        if (order.getStatus() != PurchaseOrderStatus.DRAFT && order.getStatus() != PurchaseOrderStatus.PLACED) {
            throw new InvalidPurchaseOrderStateException(
                    "Purchase order " + id + " cannot be cancelled from status " + order.getStatus());
        }
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        return purchaseOrderRepository.save(order);
    }

    public void delete(Long id) {
        purchaseOrderRepository.delete(findById(id));
    }

    private void requireStatus(PurchaseOrder order, PurchaseOrderStatus expected, String action) {
        if (order.getStatus() != expected) {
            throw new InvalidPurchaseOrderStateException(
                    "Purchase order " + order.getId() + " cannot be " + action + " from status "
                            + order.getStatus() + "; expected " + expected);
        }
    }

}
