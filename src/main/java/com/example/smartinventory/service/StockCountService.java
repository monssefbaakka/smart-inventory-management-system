package com.example.smartinventory.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.StockCountLineRequest;
import com.example.smartinventory.dto.StockCountRequest;
import com.example.smartinventory.dto.StockCountResponse;
import com.example.smartinventory.exception.InvalidStockCountStateException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockCount;
import com.example.smartinventory.model.StockCountLine;
import com.example.smartinventory.model.StockCountStatus;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.StockCountRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service running stocktakes: counting what a warehouse holds and reconciling the result.
 *
 * <p>A count collects lines while it is {@code DRAFT} and touches no stock. Completing it applies
 * every line through {@link StockMovementService} as an {@code ADJUSTMENT} against the counted
 * warehouse, so the per-warehouse level and the product's overall quantity both settle on the
 * counted figure and the correction lands in the shared movement trail.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StockCountService {

    private final StockCountRepository stockCountRepository;
    private final WarehouseService warehouseService;
    private final ProductService productService;
    private final StockLevelService stockLevelService;
    private final StockMovementService stockMovementService;

    /**
     * Opens a {@code DRAFT} count against a warehouse.
     *
     * @param request the warehouse to count and an optional note
     * @return the opened count
     * @throws ResourceNotFoundException if the warehouse does not exist
     */
    public StockCountResponse open(StockCountRequest request) {
        Warehouse warehouse = warehouseService.findById(request.warehouseId());

        StockCount count = StockCount.builder()
                .warehouse(warehouse)
                .status(StockCountStatus.DRAFT)
                .note(request.note())
                .build();

        return StockCountResponse.from(stockCountRepository.save(count));
    }

    /**
     * Records what was found on the shelf for one product, snapshotting what the warehouse was
     * believed to hold so the variance can be read before the count is applied. Counting the same
     * product again replaces the earlier line rather than raising a second one.
     *
     * @param countId identifier of the count
     * @param request the counted product and quantity
     * @return the count including the new or updated line
     * @throws ResourceNotFoundException      if the count or product does not exist
     * @throws InvalidStockCountStateException if the count is no longer {@code DRAFT}
     */
    public StockCountResponse addLine(Long countId, StockCountLineRequest request) {
        StockCount count = load(countId);
        requireDraft(count, "counted");

        Product product = productService.findById(request.productId());
        int expected = stockLevelService.quantityOnHand(product.getId(), count.getWarehouse().getId());

        count.findLineForProduct(product.getId()).ifPresentOrElse(
                line -> {
                    line.setCountedQuantity(request.countedQuantity());
                    line.setExpectedQuantity(expected);
                },
                () -> count.addLine(StockCountLine.builder()
                        .product(product)
                        .countedQuantity(request.countedQuantity())
                        .expectedQuantity(expected)
                        .build()));

        return StockCountResponse.from(stockCountRepository.save(count));
    }

    /**
     * Applies every counted line to the warehouse's stock and closes the count.
     *
     * @param id identifier of the count
     * @return the completed count
     * @throws InvalidStockCountStateException if the count is not {@code DRAFT}, or holds no lines
     */
    public StockCountResponse complete(Long id) {
        StockCount count = load(id);
        requireDraft(count, "completed");

        if (count.getLines().isEmpty()) {
            throw new InvalidStockCountStateException(
                    "Stock count " + id + " cannot be completed because nothing was counted");
        }

        for (StockCountLine line : count.getLines()) {
            stockMovementService.record(line.getProduct().getId(), count.getWarehouse().getId(),
                    MovementType.ADJUSTMENT, line.getCountedQuantity(), "Stock count #" + id);
        }

        count.setStatus(StockCountStatus.COMPLETED);
        count.setCompletedAt(Instant.now());
        return StockCountResponse.from(stockCountRepository.save(count));
    }

    /**
     * Abandons a {@code DRAFT} count, leaving stock untouched. A completed count cannot be cancelled
     * because its adjustments have already been applied.
     *
     * @param id identifier of the count
     * @return the cancelled count
     * @throws InvalidStockCountStateException if the count is not {@code DRAFT}
     */
    public StockCountResponse cancel(Long id) {
        StockCount count = load(id);
        requireDraft(count, "cancelled");

        count.setStatus(StockCountStatus.CANCELLED);
        return StockCountResponse.from(stockCountRepository.save(count));
    }

    /**
     * Returns a single count with its lines and variances.
     *
     * @param id identifier of the count
     * @return the matching count
     * @throws ResourceNotFoundException if no count carries that identifier
     */
    @Transactional(readOnly = true)
    public StockCountResponse findById(Long id) {
        return StockCountResponse.from(load(id));
    }

    /**
     * Returns counts most recent first, optionally narrowed by warehouse, by status, or by both.
     *
     * @param warehouseId identifier of a warehouse to filter by, or {@code null}
     * @param status      lifecycle status to filter by, or {@code null}
     * @return the matching counts
     */
    @Transactional(readOnly = true)
    public List<StockCountResponse> find(Long warehouseId, StockCountStatus status) {
        List<StockCount> counts;
        if (warehouseId != null) {
            warehouseService.findById(warehouseId);
            counts = status == null
                    ? stockCountRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId)
                    : stockCountRepository.findByWarehouseIdAndStatusOrderByCreatedAtDesc(warehouseId, status);
        } else if (status != null) {
            counts = stockCountRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            counts = stockCountRepository.findAllByOrderByCreatedAtDesc();
        }
        return counts.stream().map(StockCountResponse::from).toList();
    }

    private StockCount load(Long id) {
        return stockCountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock count not found with id: " + id));
    }

    private void requireDraft(StockCount count, String action) {
        if (count.getStatus() != StockCountStatus.DRAFT) {
            throw new InvalidStockCountStateException(
                    "Stock count " + count.getId() + " cannot be " + action + " from status "
                            + count.getStatus() + "; expected " + StockCountStatus.DRAFT);
        }
    }

}
