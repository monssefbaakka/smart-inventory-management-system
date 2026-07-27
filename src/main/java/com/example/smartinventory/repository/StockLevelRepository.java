package com.example.smartinventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.StockLevel;

/** Repository for per-warehouse {@link StockLevel} persistence operations. */
public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    /**
     * Finds the level held for one product in one warehouse.
     *
     * @param productId   identifier of the product
     * @param warehouseId identifier of the warehouse
     * @return the matching level, or empty when the product has never been stocked there
     */
    Optional<StockLevel> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    /**
     * Returns every stocked product in a warehouse.
     *
     * @param warehouseId identifier of the warehouse
     * @return the levels held in that warehouse
     */
    List<StockLevel> findByWarehouseId(Long warehouseId);

    /**
     * Returns a product's levels across all warehouses holding it.
     *
     * @param productId identifier of the product
     * @return the levels held for that product
     */
    List<StockLevel> findByProductId(Long productId);

}
