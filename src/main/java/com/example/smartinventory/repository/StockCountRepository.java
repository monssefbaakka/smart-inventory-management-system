package com.example.smartinventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.StockCount;
import com.example.smartinventory.model.StockCountStatus;

/** Repository for {@link StockCount} persistence operations. */
public interface StockCountRepository extends JpaRepository<StockCount, Long> {

    /**
     * Returns one page of the counts taken in a warehouse.
     *
     * @param warehouseId identifier of the warehouse
     * @param pageable    the page to return and the order to return it in
     * @return the requested page of that warehouse's counts
     */
    Page<StockCount> findByWarehouseId(Long warehouseId, Pageable pageable);

    /**
     * Returns one page of the counts sitting in one lifecycle status.
     *
     * @param status   the status to match
     * @param pageable the page to return and the order to return it in
     * @return the requested page of matching counts
     */
    Page<StockCount> findByStatus(StockCountStatus status, Pageable pageable);

    /**
     * Returns one page of the counts taken in a warehouse that sit in one lifecycle status.
     *
     * @param warehouseId identifier of the warehouse
     * @param status      the status to match
     * @param pageable    the page to return and the order to return it in
     * @return the requested page of matching counts
     */
    Page<StockCount> findByWarehouseIdAndStatus(Long warehouseId, StockCountStatus status, Pageable pageable);

}
