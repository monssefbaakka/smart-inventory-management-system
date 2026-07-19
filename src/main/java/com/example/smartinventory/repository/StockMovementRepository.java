package com.example.smartinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.StockMovement;

/** Repository for {@link StockMovement} persistence operations. */
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

}
