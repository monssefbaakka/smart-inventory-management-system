package com.example.smartinventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.Supplier;

/**
 * Repository for {@link Supplier} persistence operations.
 *
 * <p>The finders fetch the lazy {@code defaultWarehouse} association, because responses are
 * rendered from it after the transaction has closed.
 */
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    @Override
    @EntityGraph(attributePaths = "defaultWarehouse")
    Optional<Supplier> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "defaultWarehouse")
    List<Supplier> findAll();

}
