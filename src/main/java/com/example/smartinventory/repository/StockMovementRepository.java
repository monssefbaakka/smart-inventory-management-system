package com.example.smartinventory.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.StockMovement;

/** Repository for {@link StockMovement} persistence operations. */
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Returns the most recent stock movements across all products, most recent first.
     *
     * @param pageable paging/limit information
     * @return the requested page of movements ordered by creation time descending
     */
    List<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);

}
