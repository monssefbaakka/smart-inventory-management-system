package com.example.smartinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.StockCount;
import com.example.smartinventory.model.StockCountStatus;

/** Repository for {@link StockCount} persistence operations. */
public interface StockCountRepository extends JpaRepository<StockCount, Long> {

    /**
     * Returns every recorded count, most recent first.
     *
     * @return the full count history
     */
    List<StockCount> findAllByOrderByCreatedAtDesc();

    /**
     * Returns the counts taken in a warehouse, most recent first.
     *
     * @param warehouseId identifier of the warehouse
     * @return that warehouse's counts
     */
    List<StockCount> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);

    /**
     * Returns the counts sitting in one lifecycle status, most recent first.
     *
     * @param status the status to match
     * @return the matching counts
     */
    List<StockCount> findByStatusOrderByCreatedAtDesc(StockCountStatus status);

    /**
     * Returns the counts taken in a warehouse that sit in one lifecycle status, most recent first.
     *
     * @param warehouseId identifier of the warehouse
     * @param status      the status to match
     * @return the matching counts
     */
    List<StockCount> findByWarehouseIdAndStatusOrderByCreatedAtDesc(Long warehouseId, StockCountStatus status);

}
