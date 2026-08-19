package com.example.smartinventory.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.StockTransferResponse;
import com.example.smartinventory.exception.InvalidStockTransferException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.model.StockTransfer;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.StockMovementRepository;
import com.example.smartinventory.repository.StockTransferRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service moving stock between warehouses.
 *
 * <p>A transfer relocates stock rather than creating or consuming it, so the product's overall
 * quantity is deliberately left alone: only the two per-warehouse levels change, by equal and
 * opposite amounts. Both legs are also written to the movement history, so a level change is always
 * explained by a movement row.
 *
 * <p>The site the stock left is then measured against the reorder point it holds for the product: an
 * empty shelf is an empty shelf whether the goods were sold or sent on to another branch, so it both
 * alerts and orders for itself. The site it arrived at is not measured — no warehouse falls below its
 * reorder point by receiving goods.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final StockLevelService stockLevelService;
    private final AutoReorderService autoReorderService;
    private final StockEventNotificationService stockEventNotificationService;

    /**
     * Moves stock from one warehouse to another and records the move.
     *
     * <p>A move that leaves the source warehouse at or below the reorder point it holds for the
     * product notifies the configured channels for that warehouse and raises a draft order for it,
     * sized for it and delivered to it, on the same terms a stock movement does. A source warehouse
     * holding no reorder point of its own does neither: the only other figure to measure is the
     * product total, and a transfer leaves it exactly where it was.
     *
     * @param productId              identifier of the product being moved
     * @param sourceWarehouseId      identifier of the warehouse the stock leaves
     * @param destinationWarehouseId identifier of the warehouse the stock arrives at
     * @param quantity               positive number of units to move
     * @param note                   optional free-text note
     * @return the persisted transfer, flattened for the API
     * @throws ResourceNotFoundException     if the product or either warehouse does not exist
     * @throws InvalidStockTransferException if both sides name the same warehouse, or the destination
     *                                       is inactive
     * @throws com.example.smartinventory.exception.InsufficientStockException if the source warehouse
     *                                       holds fewer than {@code quantity} units
     */
    public StockTransferResponse transfer(Long productId, Long sourceWarehouseId, Long destinationWarehouseId,
            Integer quantity, String note) {
        if (sourceWarehouseId.equals(destinationWarehouseId)) {
            throw new InvalidStockTransferException(
                    "Source and destination warehouse must differ; both were " + sourceWarehouseId);
        }

        Product product = productService.findById(productId);
        Warehouse source = warehouseService.findById(sourceWarehouseId);
        Warehouse destination = warehouseService.findById(destinationWarehouseId);

        if (Boolean.FALSE.equals(destination.getActive())) {
            throw new InvalidStockTransferException(
                    "Warehouse " + destination.getCode() + " is inactive and cannot receive stock");
        }

        stockLevelService.apply(product, source, MovementType.TRANSFER_OUT, quantity);
        stockLevelService.apply(product, destination, MovementType.TRANSFER_IN, quantity);

        recordLeg(product, source, MovementType.TRANSFER_OUT, quantity, note);
        recordLeg(product, destination, MovementType.TRANSFER_IN, quantity, note);

        StockTransfer transfer = StockTransfer.builder()
                .product(product)
                .sourceWarehouse(source)
                .destinationWarehouse(destination)
                .quantity(quantity)
                .note(note)
                .build();
        StockTransferResponse saved = StockTransferResponse.from(stockTransferRepository.save(transfer));

        stockEventNotificationService.evaluateRelocation(product, source);
        autoReorderService.evaluateRelocation(product, source);

        return saved;
    }

    private void recordLeg(Product product, Warehouse warehouse, MovementType type, Integer quantity, String note) {
        stockMovementRepository.save(StockMovement.builder()
                .product(product)
                .warehouse(warehouse)
                .type(type)
                .quantity(quantity)
                .note(note)
                .build());
    }

    /**
     * Returns a single transfer.
     *
     * @param id identifier of the transfer
     * @return the matching transfer
     * @throws ResourceNotFoundException if no transfer carries that identifier
     */
    @Transactional(readOnly = true)
    public StockTransferResponse findById(Long id) {
        return stockTransferRepository.findById(id)
                .map(StockTransferResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found with id: " + id));
    }

    /**
     * Returns one page of the transfer history, optionally narrowed to one product or one warehouse.
     * A warehouse matches transfers on either side of the move.
     *
     * @param productId   identifier of a product to filter by, or {@code null}
     * @param warehouseId identifier of a warehouse to filter by, or {@code null}
     * @param pageable    the page to return and the order to return it in
     * @return the requested page of matching transfers
     */
    @Transactional(readOnly = true)
    public Page<StockTransferResponse> find(Long productId, Long warehouseId, Pageable pageable) {
        Page<StockTransfer> transfers;
        if (productId != null) {
            productService.findById(productId);
            transfers = stockTransferRepository.findByProductId(productId, pageable);
        } else if (warehouseId != null) {
            warehouseService.findById(warehouseId);
            transfers = stockTransferRepository
                    .findBySourceWarehouseIdOrDestinationWarehouseId(warehouseId, warehouseId, pageable);
        } else {
            transfers = stockTransferRepository.findAll(pageable);
        }
        return transfers.map(StockTransferResponse::from);
    }

}
