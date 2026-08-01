package com.example.smartinventory.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.ProductBatchRequest;
import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.exception.InvalidBatchException;
import com.example.smartinventory.exception.InvalidBatchStateException;
import com.example.smartinventory.exception.InvalidQueryParameterException;
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
     * Returns the lots still holding stock that expire within the next {@code days} days, so goods
     * can be sold, moved or discounted before they are worth nothing. Lots that have already
     * expired are reported by {@link #findExpired()} instead.
     *
     * @param days how far ahead to look; zero reports only what expires today
     * @return the matching lots, earliest expiry first
     * @throws InvalidQueryParameterException if {@code days} is negative
     */
    @Transactional(readOnly = true)
    public List<ProductBatch> findExpiringWithin(int days) {
        if (days < 0) {
            throw new InvalidQueryParameterException("days must not be negative, but was " + days);
        }
        LocalDate today = LocalDate.now();
        return productBatchRepository.findExpiringBetween(today, today.plusDays(days));
    }

    /**
     * Returns the lots that are past their expiry date but still hold stock: the goods that have to
     * be written off or quarantined rather than shipped.
     *
     * @return the expired lots, earliest expiry first
     */
    @Transactional(readOnly = true)
    public List<ProductBatch> findExpired() {
        return productBatchRepository.findExpiredOn(LocalDate.now());
    }

    /**
     * Adds received stock to a lot.
     *
     * @param batch     the lot the goods belong to
     * @param product   the product the movement is recorded against
     * @param warehouse the location the movement applied to, or {@code null}
     * @param quantity  units received
     * @throws InvalidBatchException if the lot belongs to another product or is held elsewhere
     */
    public void receive(ProductBatch batch, Product product, Warehouse warehouse, int quantity) {
        requireUsableFor(batch, product, warehouse);
        batch.setQuantity(batch.getQuantity() + quantity);
        productBatchRepository.save(batch);
    }

    /**
     * Removes stock from one named lot.
     *
     * @param batch     the lot the goods are taken from
     * @param product   the product the movement is recorded against
     * @param warehouse the location the movement applied to, or {@code null}
     * @param quantity  units to remove
     * @throws InvalidBatchException      if the lot belongs to another product or is held elsewhere
     * @throws InsufficientStockException if the lot holds less than {@code quantity}
     */
    public void consume(ProductBatch batch, Product product, Warehouse warehouse, int quantity) {
        requireUsableFor(batch, product, warehouse);
        if (batch.getQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Cannot remove " + quantity + " units from batch " + batch.getLotCode()
                            + ": only " + batch.getQuantity() + " in stock");
        }
        batch.setQuantity(batch.getQuantity() - quantity);
        productBatchRepository.save(batch);
    }

    /**
     * Reports whether a product has any lot still holding stock, and therefore whether an outward
     * movement that names no lot has to be allocated across lots.
     *
     * @param productId identifier of the product
     * @return {@code true} when at least one lot of the product holds stock
     */
    @Transactional(readOnly = true)
    public boolean hasStockedBatches(Long productId) {
        return !productBatchRepository.findAllocatable(productId).isEmpty();
    }

    /**
     * Takes stock out of a product's lots earliest expiry first, so the units closest to expiring
     * leave the building before the ones behind them. Lots that never expire are drawn on last.
     * Naming a warehouse restricts the allocation to the stock held there.
     *
     * @param product   the product being moved
     * @param warehouse the location the stock leaves, or {@code null} to draw on every lot
     * @param quantity  units to remove
     * @return the lots the units were taken from, in the order they were drawn on
     * @throws InsufficientStockException if the lots together hold less than {@code quantity}
     */
    public List<ProductBatch> consumeEarliestExpiryFirst(Product product, Warehouse warehouse, int quantity) {
        List<ProductBatch> candidates = warehouse == null
                ? productBatchRepository.findAllocatable(product.getId())
                : productBatchRepository.findAllocatableInWarehouse(product.getId(), warehouse.getId());

        int available = candidates.stream().mapToInt(ProductBatch::getQuantity).sum();
        if (available < quantity) {
            throw new InsufficientStockException(
                    "Cannot remove " + quantity + " units of product " + product.getId()
                            + " from its batches: only " + available + " in stock across "
                            + candidates.size() + " batches");
        }

        List<ProductBatch> drawnOn = new ArrayList<>();
        int outstanding = quantity;
        for (ProductBatch batch : candidates) {
            if (outstanding == 0) {
                break;
            }
            int taken = Math.min(batch.getQuantity(), outstanding);
            batch.setQuantity(batch.getQuantity() - taken);
            productBatchRepository.save(batch);
            drawnOn.add(batch);
            outstanding -= taken;
        }
        return drawnOn;
    }

    /**
     * Rejects a lot that cannot take part in a movement: one belonging to a different product, or
     * one held somewhere other than where the stock moved. A lot tracked without a warehouse only
     * takes part in movements recorded without one.
     *
     * @param batch     the lot named by the movement
     * @param product   the product the movement is recorded against
     * @param warehouse the location the movement applied to, or {@code null}
     * @throws InvalidBatchException if the lot cannot take part in the movement
     */
    private void requireUsableFor(ProductBatch batch, Product product, Warehouse warehouse) {
        if (!batch.getProduct().getId().equals(product.getId())) {
            throw new InvalidBatchException("Batch " + batch.getId() + " belongs to product "
                    + batch.getProduct().getId() + ", not product " + product.getId());
        }

        Long batchWarehouseId = batch.getWarehouse() == null ? null : batch.getWarehouse().getId();
        Long movementWarehouseId = warehouse == null ? null : warehouse.getId();
        if (!Objects.equals(batchWarehouseId, movementWarehouseId)) {
            throw new InvalidBatchException("Batch " + batch.getId() + " is held in warehouse "
                    + batchWarehouseId + ", but the movement applies to warehouse " + movementWarehouseId);
        }
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
