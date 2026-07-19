package com.example.smartinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockMovement;
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

    /**
     * Records a stock movement for a product and applies it to the product's quantity.
     * {@code IN} increases quantity, {@code OUT} decreases it, and {@code ADJUSTMENT}
     * sets it directly to the given value.
     *
     * @param productId identifier of the affected product
     * @param type      direction of the movement
     * @param quantity  amount moved (or the new absolute quantity for {@code ADJUSTMENT})
     * @param note      optional free-text note
     * @return the persisted movement record
     */
    public StockMovement record(Long productId, MovementType type, Integer quantity, String note) {
        Product product = productService.findById(productId);

        switch (type) {
            case IN -> product.setQuantity(product.getQuantity() + quantity);
            case OUT -> {
                if (product.getQuantity() < quantity) {
                    throw new InsufficientStockException(
                            "Cannot remove " + quantity + " units from product " + productId
                                    + ": only " + product.getQuantity() + " in stock");
                }
                product.setQuantity(product.getQuantity() - quantity);
            }
            case ADJUSTMENT -> product.setQuantity(quantity);
        }
        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .type(type)
                .quantity(quantity)
                .note(note)
                .build();
        return stockMovementRepository.save(movement);
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
