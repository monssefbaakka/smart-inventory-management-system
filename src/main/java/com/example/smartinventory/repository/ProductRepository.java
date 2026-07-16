package com.example.smartinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.smartinventory.model.Product;

/** Repository for {@link Product} persistence operations. */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Returns all products whose current quantity is at or below their reorder threshold.
     *
     * @return list of low-stock products
     */
    @Query("SELECT p FROM Product p WHERE p.quantity <= p.reorderThreshold")
    List<Product> findLowStockProducts();

}
