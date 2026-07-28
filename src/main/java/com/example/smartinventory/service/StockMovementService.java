package com.example.smartinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.exception.InvalidStockTransferException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.ProductRepository;
import com.example.smartinventory.repository.StockMovementRepository;

import lombok.RequiredArgsConstructor;

/** Service recording stock movements and applying their effect on product quantity. */
@Service
@RequiredArgsConstructor
@Transactional
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final StockLevelService stockLevelService;
    private final StockEventNotificationService stockEventNotificationService;

    /**
     * Records a stock movement for a product and applies it to the product's quantity.
     * {@code IN} increases quantity, {@code OUT} decreases it, and {@code ADJUSTMENT}
     * sets it directly to the given value.
     *
     * <p>When {@code warehouseId} is given the movement is also applied to that warehouse's stock
     * level, and the product's overall quantity moves by the same amount, so the product total stays
     * the sum of what the locations hold. Without a warehouse only the overall quantity changes.
     *
     * <p>The transfer legs are not accepted here: they always come in pairs and leave the product
     * total unchanged, so they are written by {@code StockTransferService} instead.
     *
     * @param productId   identifier of the affected product
     * @param warehouseId identifier of the location the stock moved through, or {@code null}
     * @param type        direction of the movement
     * @param quantity    amount moved (or the new absolute quantity for {@code ADJUSTMENT})
     * @param note        optional free-text note
     * @return the persisted movement record
     * @throws InvalidStockTransferException if {@code type} is one leg of a transfer
     */
    public StockMovement record(Long productId, Long warehouseId, MovementType type, Integer quantity, String note) {
        if (type.isTransferLeg()) {
            throw new InvalidStockTransferException(
                    "Movement type " + type + " is recorded by a warehouse transfer, not as a plain movement");
        }

        Product product = productService.findById(productId);
        Warehouse warehouse = warehouseId == null ? null : warehouseService.findById(warehouseId);

        if (warehouse == null) {
            applyToProduct(product, type, quantity);
        } else {
            int delta = stockLevelService.apply(product, warehouse, type, quantity);
            int total = product.getQuantity() + delta;
            if (total < 0) {
                throw new InsufficientStockException(
                        "Cannot remove " + quantity + " units from product " + productId
                                + ": only " + product.getQuantity() + " in stock");
            }
            product.setQuantity(total);
        }
        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .warehouse(warehouse)
                .type(type)
                .quantity(quantity)
                .note(note)
                .build();
        StockMovement saved = stockMovementRepository.save(movement);

        stockEventNotificationService.evaluate(product);

        return saved;
    }

    private void applyToProduct(Product product, MovementType type, Integer quantity) {
        switch (type) {
            case IN -> product.setQuantity(product.getQuantity() + quantity);
            case OUT -> {
                if (product.getQuantity() < quantity) {
                    throw new InsufficientStockException(
                            "Cannot remove " + quantity + " units from product " + product.getId()
                                    + ": only " + product.getQuantity() + " in stock");
                }
                product.setQuantity(product.getQuantity() - quantity);
            }
            case ADJUSTMENT -> product.setQuantity(quantity);
            default -> throw new IllegalArgumentException("Unsupported movement type: " + type);
        }
    }

    /**
     * Returns the movement history for a product, most recent first.
     *
     * @param productId identifier of the product
     * @return list of stock movements for the product
     */
    @Transactional(readOnly = true)
    public List<StockMovement> findByProduct(Long productId) {
        productService.findById(productId);
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

}
