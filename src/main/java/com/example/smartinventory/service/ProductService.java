package com.example.smartinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/** Service exposing CRUD operations for {@link Product}. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * Updates the mutable fields of an existing product identified by {@code id}.
     *
     * @param id             identifier of the product to update
     * @param updatedProduct product carrying the new field values
     * @return the persisted, updated product
     */
    public Product update(Long id, Product updatedProduct) {
        Product existing = findById(id);
        existing.setName(updatedProduct.getName());
        existing.setSku(updatedProduct.getSku());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setQuantity(updatedProduct.getQuantity());
        existing.setCategory(updatedProduct.getCategory());
        existing.setSupplier(updatedProduct.getSupplier());
        return productRepository.save(existing);
    }

    public void delete(Long id) {
        productRepository.delete(findById(id));
    }

}
