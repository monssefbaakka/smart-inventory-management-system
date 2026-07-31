package com.example.smartinventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.example.smartinventory.model.Product;

/**
 * Repository for {@link Product} persistence operations.
 *
 * <p>The finders fetch the lazy {@code category} and {@code supplier} associations, because
 * responses are rendered from them after the transaction has closed.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    List<Product> findAll();

    /**
     * Returns one page of the products matching {@code specification}, ordered as {@code pageable}
     * asks. The category and supplier are fetched with the page, so rendering it neither fails on a
     * proxy nor issues a query per row.
     *
     * @param specification the filters to narrow the listing by
     * @param pageable      the page to return and the order to return it in
     * @return the requested page of products
     */
    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

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
