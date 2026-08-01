package com.example.smartinventory.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.ProductSearchCriteria;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.AuditAction;
import com.example.smartinventory.model.Category;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.repository.ProductRepository;
import com.example.smartinventory.repository.ProductSpecifications;

import lombok.RequiredArgsConstructor;

/** Service exposing CRUD operations for {@link Product}. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    /** Entity type name recorded in the audit log for product mutations. */
    static final String AUDIT_ENTITY_TYPE = "Product";

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final SupplierService supplierService;
    private final AuditService auditService;

    /**
     * Persists a new product and records a {@code CREATE} audit entry.
     *
     * @param product the product to create
     * @return the persisted product
     */
    public Product create(Product product) {
        product.setCategory(resolveCategory(product.getCategory()));
        product.setSupplier(resolveSupplier(product.getSupplier()));
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

    /**
     * Returns one page of the products matching {@code criteria}.
     *
     * @param criteria the filters to narrow the listing by
     * @param pageable the page to return and the order to return it in
     * @return the requested page of products
     */
    @Transactional(readOnly = true)
    public Page<Product> search(ProductSearchCriteria criteria, Pageable pageable) {
        return productRepository.findAll(ProductSpecifications.matching(criteria), pageable);
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
        existing.setReorderQuantity(updatedProduct.getReorderQuantity());
        existing.setCategory(resolveCategory(updatedProduct.getCategory()));
        existing.setSupplier(resolveSupplier(updatedProduct.getSupplier()));
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

    /**
     * Replaces the category carried by a request payload with the persisted one it names, so the
     * product holds a managed reference whose name can be rendered into the response. A payload
     * naming no category, or naming one without an id, leaves the product uncategorised.
     *
     * @param requested the category as sent by the caller, possibly {@code null}
     * @return the persisted category, or {@code null} when none was named
     * @throws ResourceNotFoundException if the named category does not exist
     */
    private Category resolveCategory(Category requested) {
        if (requested == null || requested.getId() == null) {
            return null;
        }
        return categoryService.findById(requested.getId());
    }

    /**
     * Replaces the supplier carried by a request payload with the persisted one it names.
     *
     * @param requested the supplier as sent by the caller, possibly {@code null}
     * @return the persisted supplier, or {@code null} when none was named
     * @throws ResourceNotFoundException if the named supplier does not exist
     */
    private Supplier resolveSupplier(Supplier requested) {
        if (requested == null || requested.getId() == null) {
            return null;
        }
        return supplierService.findById(requested.getId());
    }

}
