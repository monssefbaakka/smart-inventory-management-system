package com.example.smartinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.PurchaseOrder;

/** Repository for {@link PurchaseOrder} persistence operations. */
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    /**
     * Returns all purchase orders raised against a supplier, most recent first.
     *
     * @param supplierId identifier of the supplier
     * @return the supplier's purchase orders, newest first
     */
    List<PurchaseOrder> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

}
