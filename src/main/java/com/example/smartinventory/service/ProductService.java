package com.example.smartinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.AuditAction;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/** Service exposing CRUD operations for {@link Product}. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    /** Entity type name recorded in the audit log for product mutations. */
    static final String AUDIT_ENTITY_TYPE = "Product";

    private final ProductRepository productRepository;
    private final AuditService auditService;

    /**
     * Persists a new product and records a {@code CREATE} audit entry.
     *
     * @param product the product to create
     * @return the persisted product
     */
    public Product create(Product product) {
        Product saved = productRepository.save(product);
        auditService.record(AUDIT_ENTITY_TYPE, saved.getId(), AuditAction.CREATE);
        return saved;
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    /**
     * Resolves a scanned barcode to the product carrying it.
     *
     * @param barcode the symbol content read from a scanner
     * @return the matching product
     * @throws ResourceNotFoundException if no product carries that barcode
     */
    @Transactional(readOnly = true)
    public Product findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with barcode: " + barcode));
    }

    /**
     * Returns the content encoded into a product's printed symbols: its barcode when one is
     * assigned, otherwise its SKU, so every product can carry a scannable label.
     *
     * @param id identifier of the product
     * @return the text to encode
     */
    @Transactional(readOnly = true)
    public String symbolContent(Long id) {
        Product product = findById(id);
        String barcode = product.getBarcode();
        return barcode == null || barcode.isBlank() ? product.getSku() : barcode;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * Returns all products whose stock quantity is at or below their reorder threshold.
     *
     * @return list of low-stock products requiring attention
     */
    @Transactional(readOnly = true)
    public List<Product> findLowStockProducts() {
        return productRepository.findLowStockProducts();
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
        existing.setBarcode(updatedProduct.getBarcode());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setQuantity(updatedProduct.getQuantity());
        if (updatedProduct.getReorderThreshold() != null) {
            existing.setReorderThreshold(updatedProduct.getReorderThreshold());
        }
        existing.setCategory(updatedProduct.getCategory());
        existing.setSupplier(updatedProduct.getSupplier());
        Product saved = productRepository.save(existing);
        auditService.record(AUDIT_ENTITY_TYPE, saved.getId(), AuditAction.UPDATE);
        return saved;
    }

    /**
     * Deletes the product identified by {@code id} and records a {@code DELETE} audit entry.
     *
     * @param id identifier of the product to delete
     */
    public void delete(Long id) {
        Product existing = findById(id);
        productRepository.delete(existing);
        auditService.record(AUDIT_ENTITY_TYPE, existing.getId(), AuditAction.DELETE);
    }

}
