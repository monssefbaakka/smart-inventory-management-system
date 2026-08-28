package com.example.smartinventory.repository;

import java.time.LocalDate;
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
     * Returns one page of the purchase orders whose goods were due before a given day and are still
     * being waited on.
     *
     * <p>The statuses are passed in rather than assumed here: what counts as still waiting is the
     * order's business, not the query's. An order carrying no expected delivery date is left out by
     * the comparison itself, which is what a promise nobody made deserves.
     *
     * @param statuses the order statuses that count as awaiting delivery
     * @param onDate   the day they had to have been due before
     * @param pageable the page to return and the order to return it in
     * @return the requested page of late orders
     */
    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    Page<PurchaseOrder> findByStatusInAndExpectedDeliveryDateBefore(Collection<PurchaseOrderStatus> statuses,
            LocalDate onDate, Pageable pageable);

    /**
     * Returns one page of the late orders raised against one supplier, for the question that follows
     * "what is late": which of them is this supplier sitting on.
     *
     * @param supplierId identifier of the supplier
     * @param statuses   the order statuses that count as awaiting delivery
     * @param onDate     the day they had to have been due before
     * @param pageable   the page to return and the order to return it in
     * @return the requested page of that supplier's late orders
     */
    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    Page<PurchaseOrder> findBySupplierIdAndStatusInAndExpectedDeliveryDateBefore(Long supplierId,
            Collection<PurchaseOrderStatus> statuses, LocalDate onDate, Pageable pageable);

    /**
     * Returns the orders of one supplier whose delivery can be judged against what was promised:
     * fulfilled, and carrying both the day they were due and the day they arrived.
     *
     * <p>Everything else says nothing about whether the supplier keeps dates. An order still awaiting
     * delivery has not arrived, one cancelled part-delivered was abandoned rather than delivered
     * late, one placed against a supplier naming no lead time was promised nothing, and one received
     * before arrivals were recorded has only half the pair.
     *
     * <p>The promise read is the one the order was placed on, which is the one the order judges
     * itself by. An order placed against no date and given one later by being re-promised carries an
     * expected date and no original: the supplier promised nothing to miss, and it is not judged.
     *
     * <p>The orders are read rather than the two dates alone, because how a delivery went against its
     * promise is the order's own arithmetic and is stated once, on the order. Their lines come with
     * them, because what the record is worth is the order's own total and reaching for a lazy
     * collection per row would be a query per delivery.
     *
     * @param supplierId identifier of the supplier
     * @param status     the status a fulfilled order is in
     * @return that supplier's judgeable deliveries, in no particular order
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("select o from PurchaseOrder o where o.supplier.id = :supplierId and o.status = :status "
            + "and o.originalExpectedDeliveryDate is not null and o.deliveredDate is not null")
    List<PurchaseOrder> findJudgeableDeliveries(@Param("supplierId") Long supplierId,
            @Param("status") PurchaseOrderStatus status);

    /**
     * Returns every judgeable delivery, whoever supplied it, for reading the whole book of suppliers
     * in one pass rather than asking after each of them in turn.
     *
     * <p>The supplier is fetched with the order, because the deliveries are grouped by supplier
     * afterwards and reaching for a lazy association per row would be a query per order. The lines
     * come with them for the same reason: the money on each row is summed from them.
     *
     * @param status the status a fulfilled order is in
     * @return every judgeable delivery, in no particular order
     */
    @EntityGraph(attributePaths = {"supplier", "items"})
    @Query("select o from PurchaseOrder o where o.status = :status "
            + "and o.originalExpectedDeliveryDate is not null and o.deliveredDate is not null")
    List<PurchaseOrder> findJudgeableDeliveries(@Param("status") PurchaseOrderStatus status);

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
