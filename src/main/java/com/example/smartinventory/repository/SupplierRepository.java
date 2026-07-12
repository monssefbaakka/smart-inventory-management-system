package com.example.smartinventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.Supplier;

/** Repository for {@link Supplier} persistence operations. */
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

}
