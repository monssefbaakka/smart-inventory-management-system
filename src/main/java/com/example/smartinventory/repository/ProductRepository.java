package com.example.smartinventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.Product;

/** Repository for {@link Product} persistence operations. */
public interface ProductRepository extends JpaRepository<Product, Long> {

}
