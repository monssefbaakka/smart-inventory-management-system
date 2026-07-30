package com.example.smartinventory.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.StockMovement;

/**
 * Repository for {@link StockMovement} persistence operations.
 *
 * <p>The finders fetch the lazy {@code product} and {@code warehouse} associations, because
 * responses are rendered from them after the transaction has closed.
 */
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @EntityGraph(attributePaths = {"product", "warehouse"})
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Returns the most recent stock movements across all products, most recent first.
     *
     * @param pageable paging/limit information
     * @return the requested page of movements ordered by creation time descending
     */
    @EntityGraph(attributePaths = {"product", "warehouse"})
    List<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);

}
