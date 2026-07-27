package com.example.smartinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.WarehouseRepository;

import lombok.RequiredArgsConstructor;

/** Service exposing CRUD operations for {@link Warehouse}. */
@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public Warehouse create(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    @Transactional(readOnly = true)
    public Warehouse findById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
    }

    /**
     * Finds a warehouse by its business code, as printed on labels and documents.
     *
     * @param code the unique warehouse code
     * @return the matching warehouse
     * @throws ResourceNotFoundException if no warehouse carries that code
     */
    @Transactional(readOnly = true)
    public Warehouse findByCode(String code) {
        return warehouseRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with code: " + code));
    }

    @Transactional(readOnly = true)
    public List<Warehouse> findAll() {
        return warehouseRepository.findAll();
    }

    /**
     * Updates the mutable fields of an existing warehouse identified by {@code id}.
     *
     * @param id               identifier of the warehouse to update
     * @param updatedWarehouse warehouse carrying the new field values
     * @return the persisted, updated warehouse
     */
    public Warehouse update(Long id, Warehouse updatedWarehouse) {
        Warehouse existing = findById(id);
        existing.setCode(updatedWarehouse.getCode());
        existing.setName(updatedWarehouse.getName());
        existing.setLocation(updatedWarehouse.getLocation());
        if (updatedWarehouse.getActive() != null) {
            existing.setActive(updatedWarehouse.getActive());
        }
        return warehouseRepository.save(existing);
    }

    public void delete(Long id) {
        warehouseRepository.delete(findById(id));
    }

}
