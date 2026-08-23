package com.example.smartinventory.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.model.PurchaseOrderStatus;

/**
 * Repository for {@link PurchaseOrder} persistence operations.
 *
 * <p>The finders fetch the lazy {@code supplier}, {@code warehouse} and {@code items}
 * associations, because responses are rendered from them after the transaction has closed.
 */
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"supplier", "warehouse", "items", "items.product"})
    Optional<PurchaseOrder> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"supplier", "warehouse", "items", "items.product"})
    List<PurchaseOrder> findAll();

    /**
     * Returns one page of every purchase order.
     *
     * <p>Only the {@code supplier} and the delivery {@code warehouse} are fetched: fetching the
     * {@code items} collection alongside a page would make Hibernate apply the paging in memory,
     * over every row the query matched.
     *
     * @param pageable the page to return and the order to return it in
     * @return the requested page of orders
     */
    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    Page<PurchaseOrder> findAllBy(Pageable pageable);

    /**
     * Returns one page of the purchase orders raised against a supplier.
     *
     * @param supplierId identifier of the supplier
     * @param pageable   the page to return and the order to return it in
     * @return the requested page of that supplier's orders
     */
    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    Page<PurchaseOrder> findBySupplierId(Long supplierId, Pageable pageable);

    /**
     * Sums how much of a product is bought but not yet delivered, so replenishment is measured
     * against the cover that is genuinely on its way rather than against the bare fact of an order.
     *
     * <p>Each line counts what was ordered less what has already arrived: the received part is on the
     * shelf and already counted there, and only the remainder is still coming. An order in a status
     * outside {@code statuses} — cancelled, or received in full — has nothing left to deliver and
     * counts nothing.
     *
     * @param statuses  the order statuses that count as still open
     * @param productId identifier of the product to look for among the line items
     * @return the units still to arrive, or zero when none are
     */
    @Query("select coalesce(sum(i.quantity - i.receivedQuantity), 0L) from PurchaseOrder o join o.items i "
            + "where o.status in :statuses and i.product.id = :productId")
    long sumOutstandingForProduct(@Param("statuses") Collection<PurchaseOrderStatus> statuses,
            @Param("productId") Long productId);

    /**
     * Sums how much of a product is bought but not yet delivered to one warehouse, for a site that
     * measures its own stock against a reorder point of its own.
     *
     * <p>An open order heading somewhere else does not count: goods delivered to another site do not
     * fill this one's shelves, and a site short at the same time as its neighbour is short in its own
     * right.
     *
     * @param statuses    the order statuses that count as still open
     * @param warehouseId identifier of the warehouse the order must be delivered to
     * @param productId   identifier of the product to look for among the line items
     * @return the units still to arrive at that warehouse, or zero when none are
     */
    @Query("select coalesce(sum(i.quantity - i.receivedQuantity), 0L) from PurchaseOrder o join o.items i "
            + "where o.status in :statuses and o.warehouse.id = :warehouseId and i.product.id = :productId")
    long sumOutstandingForProductInWarehouse(@Param("statuses") Collection<PurchaseOrderStatus> statuses,
            @Param("warehouseId") Long warehouseId, @Param("productId") Long productId);

}
