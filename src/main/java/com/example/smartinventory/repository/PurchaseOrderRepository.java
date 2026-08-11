package com.example.smartinventory.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * Reports whether a product is already on order, so replenishment is not raised twice for
     * the same shortfall.
     *
     * @param statuses the order statuses that count as still open
     * @param productId identifier of the product to look for among the line items
     * @return {@code true} if some order in one of those statuses carries the product
     */
    boolean existsByStatusInAndItemsProductId(Collection<PurchaseOrderStatus> statuses, Long productId);

}
