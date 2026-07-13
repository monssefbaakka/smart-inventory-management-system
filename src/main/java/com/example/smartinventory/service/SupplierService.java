package com.example.smartinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

/** Service exposing CRUD operations for {@link Supplier}. */
@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public Supplier create(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    @Transactional(readOnly = true)
    public Supplier findById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        return supplierRepository.findAll();
    }

    /**
     * Updates the mutable fields of an existing supplier identified by {@code id}.
     *
     * @param id              identifier of the supplier to update
     * @param updatedSupplier supplier carrying the new field values
     * @return the persisted, updated supplier
     */
    public Supplier update(Long id, Supplier updatedSupplier) {
        Supplier existing = findById(id);
        existing.setName(updatedSupplier.getName());
        existing.setContactName(updatedSupplier.getContactName());
        existing.setEmail(updatedSupplier.getEmail());
        existing.setPhone(updatedSupplier.getPhone());
        existing.setAddress(updatedSupplier.getAddress());
        return supplierRepository.save(existing);
    }

    public void delete(Long id) {
        supplierRepository.delete(findById(id));
    }

}
