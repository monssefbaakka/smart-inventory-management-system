package com.example.smartinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.StockLevelResponse;
import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockLevel;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.StockLevelRepository;

import lombok.RequiredArgsConstructor;

/** Service maintaining and reading per-warehouse stock levels. */
@Service
@RequiredArgsConstructor
@Transactional
public class StockLevelService {

    private final StockLevelRepository stockLevelRepository;
    private final ProductService productService;
    private final WarehouseService warehouseService;

    /**
     * Applies a movement to one warehouse's level for a product, creating the level on first use.
     * {@code IN} and {@code TRANSFER_IN} add, {@code OUT} and {@code TRANSFER_OUT} remove, and
     * {@code ADJUSTMENT} sets the level to the given value.
     *
     * @param product   the product being moved
     * @param warehouse the location holding the stock
     * @param type      direction of the movement
     * @param quantity  amount moved (or the new absolute quantity for {@code ADJUSTMENT})
     * @return the change applied to the level; for the plain movement types the caller mirrors it onto
     *         the product's overall quantity. Negative when stock left the warehouse.
     */
    public int apply(Product product, Warehouse warehouse, MovementType type, Integer quantity) {
        StockLevel level = stockLevelRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> StockLevel.builder().product(product).warehouse(warehouse).quantity(0).build());

        int current = level.getQuantity() == null ? 0 : level.getQuantity();
        int delta = switch (type) {
            case IN, TRANSFER_IN -> quantity;
            case OUT, TRANSFER_OUT -> {
                if (current < quantity) {
                    throw new InsufficientStockException(
                            "Cannot remove " + quantity + " units of product " + product.getId()
                                    + " from warehouse " + warehouse.getCode() + ": only " + current + " in stock");
                }
                yield -quantity;
            }
            case ADJUSTMENT -> quantity - current;
        };

        level.setQuantity(current + delta);
        stockLevelRepository.save(level);
        return delta;
    }

    /**
     * Reports how much of a product a warehouse is believed to hold right now.
     *
     * @param productId   identifier of the product
     * @param warehouseId identifier of the warehouse
     * @return the quantity held, or zero when the product has never been stocked there
     */
    @Transactional(readOnly = true)
    public int quantityOnHand(Long productId, Long warehouseId) {
        return stockLevelRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .map(level -> level.getQuantity() == null ? 0 : level.getQuantity())
                .orElse(0);
    }

    /**
     * Returns every stocked product in a warehouse.
     *
     * @param warehouseId identifier of the warehouse
     * @return the levels held there
     */
    @Transactional(readOnly = true)
    public List<StockLevelResponse> findByWarehouse(Long warehouseId) {
        warehouseService.findById(warehouseId);
        return stockLevelRepository.findByWarehouseId(warehouseId).stream()
                .map(StockLevelResponse::from)
                .toList();
    }

    /**
     * Returns a product's stock broken down by warehouse.
     *
     * @param productId identifier of the product
     * @return the levels held for that product
     */
    @Transactional(readOnly = true)
    public List<StockLevelResponse> findByProduct(Long productId) {
        productService.findById(productId);
        return stockLevelRepository.findByProductId(productId).stream()
                .map(StockLevelResponse::from)
                .toList();
    }

}
