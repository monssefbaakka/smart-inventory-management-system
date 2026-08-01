package com.example.smartinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.ProductBatchRequest;
import com.example.smartinventory.exception.InvalidBatchStateException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.ProductBatch;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.ProductBatchRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service tracking the batches (lots) a product's stock is made of.
 *
 * <p>Batches are declared empty and filled by stock movements, so what a lot holds is always
 * explained by the movement history rather than set directly.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductBatchService {

    private final ProductBatchRepository productBatchRepository;
    private final ProductService productService;
    private final WarehouseService warehouseService;

    /**
     * Starts tracking a lot of a product. The lot begins empty; stock reaches it through movements
     * naming it.
     *
     * @param productId identifier of the product the lot belongs to
     * @param request   the lot code, optional expiry date and optional warehouse
     * @return the persisted batch
     * @throws ResourceNotFoundException  if the product or the named warehouse does not exist
     * @throws InvalidBatchStateException if the product already carries that lot code
     */
    public ProductBatch create(Long productId, ProductBatchRequest request) {
        Product product = productService.findById(productId);
        if (productBatchRepository.existsByProductIdAndLotCode(productId, request.lotCode())) {
            throw new InvalidBatchStateException(
                    "Product " + productId + " already has a batch with lot code " + request.lotCode());
        }

        Warehouse warehouse = request.warehouseId() == null
                ? null
                : warehouseService.findById(request.warehouseId());

        return productBatchRepository.save(ProductBatch.builder()
                .product(product)
                .warehouse(warehouse)
                .lotCode(request.lotCode())
                .expiryDate(request.expiryDate())
                .quantity(0)
                .build());
    }

    @Transactional(readOnly = true)
    public ProductBatch findById(Long id) {
        return productBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));
    }

    /**
     * Returns every lot of a product, whether or not it still holds stock, earliest expiry first.
     *
     * @param productId identifier of the product
     * @return the product's lots
     * @throws ResourceNotFoundException if the product does not exist
     */
    @Transactional(readOnly = true)
    public List<ProductBatch> findByProduct(Long productId) {
        productService.findById(productId);
        return productBatchRepository.findByProduct(productId);
    }

    /**
     * Stops tracking an empty lot. A lot still holding stock cannot be deleted: that stock exists
     * and has to leave through a movement, not through the disappearance of the record explaining
     * where it came from.
     *
     * @param id identifier of the batch
     * @throws ResourceNotFoundException  if the batch does not exist
     * @throws InvalidBatchStateException if the batch still holds stock
     */
    public void delete(Long id) {
        ProductBatch batch = findById(id);
        if (batch.getQuantity() != null && batch.getQuantity() > 0) {
            throw new InvalidBatchStateException(
                    "Batch " + id + " still holds " + batch.getQuantity() + " units and cannot be deleted");
        }
        productBatchRepository.delete(batch);
    }

}
