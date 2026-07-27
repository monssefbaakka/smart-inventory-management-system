package com.example.smartinventory.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.Warehouse;

/** Repository for {@link Warehouse} persistence operations. */
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    /**
     * Finds a warehouse by its business code.
     *
     * @param code the unique warehouse code
     * @return the matching warehouse, or empty when no warehouse carries that code
     */
    Optional<Warehouse> findByCode(String code);

    /**
     * Reports whether a warehouse already uses the given code.
     *
     * @param code the code to check
     * @return {@code true} when the code is taken
     */
    boolean existsByCode(String code);

}
