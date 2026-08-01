package com.example.smartinventory.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.StockMovement;

/**
 * Repository for {@link StockMovement} persistence operations.
 *
 * <p>The finders fetch the lazy {@code product}, {@code warehouse} and {@code batch} associations,
 * because responses are rendered from them after the transaction has closed.
 */
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * Returns one page of a product's movement history, ordered as {@code pageable} asks.
     *
     * @param productId identifier of the product
     * @param pageable  the page to return and the order to return it in
     * @return the requested page of that product's movements
     */
    @EntityGraph(attributePaths = {"product", "warehouse", "batch"})
    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    /**
     * Returns the most recent stock movements across all products, most recent first.
     *
     * @param pageable paging/limit information
     * @return the requested page of movements ordered by creation time descending
     */
    @EntityGraph(attributePaths = {"product", "warehouse", "batch"})
    List<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);

}
