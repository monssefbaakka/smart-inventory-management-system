package com.example.smartinventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.PurchaseOrder;

/**
 * Repository for {@link PurchaseOrder} persistence operations.
 *
 * <p>The finders fetch the lazy {@code supplier} and {@code items} associations, because
 * responses are rendered from them after the transaction has closed.
 */
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"supplier", "items", "items.product"})
    Optional<PurchaseOrder> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"supplier", "items", "items.product"})
    List<PurchaseOrder> findAll();

    /**
     * Returns all purchase orders raised against a supplier, most recent first.
     *
     * @param supplierId identifier of the supplier
     * @return the supplier's purchase orders, newest first
     */
    @EntityGraph(attributePaths = {"supplier", "items", "items.product"})
    List<PurchaseOrder> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

}
