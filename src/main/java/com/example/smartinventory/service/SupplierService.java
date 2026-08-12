package com.example.smartinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

/** Service exposing CRUD operations for {@link Supplier}. */
@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final WarehouseService warehouseService;

    /**
     * Persists a new supplier, resolving the warehouse its goods are normally delivered to.
     *
     * @param supplier the supplier to create
     * @return the persisted supplier
     * @throws ResourceNotFoundException if the named default warehouse does not exist
     */
    public Supplier create(Supplier supplier) {
        supplier.setDefaultWarehouse(resolveDefaultWarehouse(supplier.getDefaultWarehouse()));
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
     * <p>Changing where the supplier's goods are normally delivered applies to the orders raised
     * from here on. Orders already raised recorded the warehouse they were given and keep it: where
     * a delivery is going was settled when the order went out to the supplier.
     *
     * @param id              identifier of the supplier to update
     * @param updatedSupplier supplier carrying the new field values
     * @return the persisted, updated supplier
     * @throws ResourceNotFoundException if the supplier or the named default warehouse does not exist
     */
    public Supplier update(Long id, Supplier updatedSupplier) {
        Supplier existing = findById(id);
        existing.setName(updatedSupplier.getName());
        existing.setContactName(updatedSupplier.getContactName());
        existing.setEmail(updatedSupplier.getEmail());
        existing.setPhone(updatedSupplier.getPhone());
        existing.setAddress(updatedSupplier.getAddress());
        existing.setDefaultWarehouse(resolveDefaultWarehouse(updatedSupplier.getDefaultWarehouse()));
        return supplierRepository.save(existing);
    }

    public void delete(Long id) {
        supplierRepository.delete(findById(id));
    }

    /**
     * Replaces the warehouse carried by a request payload with the persisted one it names, so the
     * supplier holds a managed reference whose code can be rendered into the response. A payload
     * naming no warehouse, or naming one without an id, leaves the supplier with no usual
     * destination.
     *
     * @param requested the warehouse as sent by the caller, possibly {@code null}
     * @return the persisted warehouse, or {@code null} when none was named
     * @throws ResourceNotFoundException if the named warehouse does not exist
     */
    private Warehouse resolveDefaultWarehouse(Warehouse requested) {
        if (requested == null || requested.getId() == null) {
            return null;
        }
        return warehouseService.findById(requested.getId());
    }

}
