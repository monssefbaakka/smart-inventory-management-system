package com.example.smartinventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.smartinventory.model.Product;

/**
 * Repository for {@link Product} persistence operations.
 *
 * <p>The finders fetch the lazy {@code category} and {@code supplier} associations, because
 * responses are rendered from them after the transaction has closed.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    List<Product> findAll();

    /**
     * Returns all products whose current quantity is at or below their reorder threshold.
     *
     * @return list of low-stock products
     */
    @Query("SELECT p FROM Product p WHERE p.quantity <= p.reorderThreshold")
    @EntityGraph(attributePaths = {"category", "supplier"})
    List<Product> findLowStockProducts();

    /**
     * Finds the product carrying the given scanned barcode.
     *
     * @param barcode the symbol content read from a scanner
     * @return the matching product, or empty when no product carries that barcode
     */
    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findByBarcode(String barcode);

}
