package com.example.smartinventory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.exception.InvalidBatchException;
import com.example.smartinventory.exception.InvalidStockTransferException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.ProductBatch;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.ProductRepository;
import com.example.smartinventory.repository.StockMovementRepository;

import lombok.RequiredArgsConstructor;

/** Service recording stock movements and applying their effect on product quantity. */
@Service
@RequiredArgsConstructor
@Transactional
public class StockMovementService {

    /**
     * Decimal places kept on a cost. Four rather than two because an average is a quotient: rounding
     * it to the penny at every receipt would drift away from what the stock actually cost.
     */
    static final int COST_SCALE = 4;

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final StockLevelService stockLevelService;
    private final StockEventNotificationService stockEventNotificationService;
    private final AutoReorderService autoReorderService;
    private final ProductBatchService productBatchService;

    /**
     * Records a stock movement for a product and applies it to the product's quantity.
     * {@code IN} increases quantity, {@code OUT} decreases it, and {@code ADJUSTMENT}
     * sets it directly to the given value.
     *
     * <p>When {@code warehouseId} is given the movement is also applied to that warehouse's stock
     * level, and the product's overall quantity moves by the same amount, so the product total stays
     * the sum of what the locations hold. Without a warehouse only the overall quantity changes.
     *
     * <p>Once the movement is written the resulting stock level is evaluated: a level at or below the
     * product's reorder threshold notifies the configured channels and, when the automatic reorder is
     * enabled, raises a draft purchase order for the product. A movement through a warehouse that
     * holds its own reorder point for the product is judged against that site instead, and orders
     * for it.
     *
     * <p>The transfer legs are not accepted here: they always come in pairs and leave the product
     * total unchanged, so they are written by {@code StockTransferService} instead.
     *
     * @param productId   identifier of the affected product
     * @param warehouseId identifier of the location the stock moved through, or {@code null}
     * @param batchId     identifier of the lot the stock belongs to, or {@code null}
     * @param type        direction of the movement
     * @param quantity    amount moved (or the new absolute quantity for {@code ADJUSTMENT})
     * @param note        optional free-text note
     * @return the persisted movement record
     * @throws InvalidStockTransferException if {@code type} is one leg of a transfer
     * @throws InvalidBatchException         if an {@code ADJUSTMENT} names a lot, or the named lot
     *                                       cannot take part in the movement
     */
    public StockMovement record(Long productId, Long warehouseId, Long batchId, MovementType type, Integer quantity,
            String note) {
        return record(productId, warehouseId, batchId, type, quantity, note, null);
    }

    /**
     * Records a stock movement as {@link #record(Long, Long, Long, MovementType, Integer, String)}
     * does, valuing it at a stated cost per unit.
     *
     * <p>A receipt carrying a {@code unitCost} rolls the product's weighted average cost:
     * {@code (onHand x average + received x unitCost) / (onHand + received)}. A receipt without one
     * is taken in at the running average and leaves it where it was, and an outward movement is
     * always valued at the average of the moment — that figure is the cost of the goods sold, fixed
     * when the stock leaves and untouched by whatever is received afterwards.
     *
     * <p>An {@code ADJUSTMENT} ignores a stated cost: a recount settles how many units are on the
     * shelf, not what they cost, so it is valued at the running average like any other outcome of a
     * count.
     *
     * @param productId   identifier of the affected product
     * @param warehouseId identifier of the location the stock moved through, or {@code null}
     * @param batchId     identifier of the lot the stock belongs to, or {@code null}
     * @param type        direction of the movement
     * @param quantity    amount moved (or the new absolute quantity for {@code ADJUSTMENT})
     * @param note        optional free-text note
     * @param unitCost    cost of one received unit, or {@code null} to value the movement at the
     *                    product's current average cost
     * @return the persisted movement record
     * @throws InvalidStockTransferException if {@code type} is one leg of a transfer
     * @throws InvalidBatchException         if an {@code ADJUSTMENT} names a lot, or the named lot
     *                                       cannot take part in the movement
     */
    public StockMovement record(Long productId, Long warehouseId, Long batchId, MovementType type, Integer quantity,
            String note, BigDecimal unitCost) {
        if (type.isTransferLeg()) {
            throw new InvalidStockTransferException(
                    "Movement type " + type + " is recorded by a warehouse transfer, not as a plain movement");
        }
        if (batchId != null && type == MovementType.ADJUSTMENT) {
            throw new InvalidBatchException(
                    "An ADJUSTMENT sets an absolute quantity and cannot name a batch; correct a lot by moving "
                            + "stock in or out of it");
        }

        Product product = productService.findById(productId);
        Warehouse warehouse = warehouseId == null ? null : warehouseService.findById(warehouseId);
        ProductBatch batch = batchId == null ? null : productBatchService.findById(batchId);

        BigDecimal valuedAt = applyCost(product, type, quantity, unitCost);

        if (warehouse == null) {
            applyToProduct(product, type, quantity);
        } else {
            int delta = stockLevelService.apply(product, warehouse, type, quantity);
            int total = product.getQuantity() + delta;
            if (total < 0) {
                throw new InsufficientStockException(
                        "Cannot remove " + quantity + " units from product " + productId
                                + ": only " + product.getQuantity() + " in stock");
            }
            product.setQuantity(total);
        }
        productRepository.save(product);
        applyToBatches(product, warehouse, batch, type, quantity);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .warehouse(warehouse)
                .batch(batch)
                .type(type)
                .quantity(quantity)
                .unitCost(valuedAt)
                .totalCost(valuedAt.multiply(BigDecimal.valueOf(quantity)))
                .note(note)
                .build();
        StockMovement saved = stockMovementRepository.save(movement);

        stockEventNotificationService.evaluate(product);
        autoReorderService.evaluate(product, warehouse);

        return saved;
    }

    /**
     * Values the movement and, for a receipt that states what the goods cost, rolls the product's
     * weighted average cost forward before the quantity moves — the old average still has to be
     * weighted by the stock it belonged to.
     *
     * <p>A receipt of goods the product has none of takes its stated cost as the average outright,
     * which is also what the formula gives once the weight of nothing is nothing.
     *
     * @param product  the product being moved
     * @param type     direction of the movement
     * @param quantity amount moved
     * @param unitCost stated cost of one received unit, or {@code null}
     * @return the cost one unit of this movement is valued at
     */
    private BigDecimal applyCost(Product product, MovementType type, Integer quantity, BigDecimal unitCost) {
        BigDecimal average = product.getAverageCost() == null ? BigDecimal.ZERO : product.getAverageCost();
        if (type != MovementType.IN || unitCost == null) {
            return average;
        }

        int onHand = Math.max(product.getQuantity(), 0);
        BigDecimal received = BigDecimal.valueOf(quantity);
        BigDecimal rolled = average.multiply(BigDecimal.valueOf(onHand)).add(unitCost.multiply(received))
                .divide(BigDecimal.valueOf(onHand).add(received), COST_SCALE, RoundingMode.HALF_UP);
        product.setAverageCost(rolled);
        return unitCost;
    }

    /**
     * Applies the movement to the product's lots. A movement naming one adds to or takes from that
     * lot; an outward movement naming none is spread across the product's lots earliest expiry
     * first, but only once the product actually has lots, so products tracked without batches keep
     * behaving exactly as they did before.
     *
     * <p>An {@code ADJUSTMENT} leaves the lots untouched: it sets an absolute quantity, and which
     * lots that figure belongs to is a question only a stocktake can answer.
     *
     * @param product   the product being moved
     * @param warehouse the location the movement applied to, or {@code null}
     * @param batch     the lot named by the movement, or {@code null}
     * @param type      direction of the movement
     * @param quantity  amount moved
     */
    private void applyToBatches(Product product, Warehouse warehouse, ProductBatch batch, MovementType type,
            Integer quantity) {
        switch (type) {
            case IN -> {
                if (batch != null) {
                    productBatchService.receive(batch, product, warehouse, quantity);
                }
            }
            case OUT -> {
                if (batch != null) {
                    productBatchService.consume(batch, product, warehouse, quantity);
                } else if (productBatchService.hasStockedBatches(product.getId())) {
                    productBatchService.consumeEarliestExpiryFirst(product, warehouse, quantity);
                }
            }
            default -> { }
        }
    }

    private void applyToProduct(Product product, MovementType type, Integer quantity) {
        switch (type) {
            case IN -> product.setQuantity(product.getQuantity() + quantity);
            case OUT -> {
                if (product.getQuantity() < quantity) {
                    throw new InsufficientStockException(
                            "Cannot remove " + quantity + " units from product " + product.getId()
                                    + ": only " + product.getQuantity() + " in stock");
                }
                product.setQuantity(product.getQuantity() - quantity);
            }
            case ADJUSTMENT -> product.setQuantity(quantity);
            default -> throw new IllegalArgumentException("Unsupported movement type: " + type);
        }
    }

    /**
     * Returns one page of the movement history for a product.
     *
     * @param productId identifier of the product
     * @param pageable  the page to return and the order to return it in
     * @return the requested page of the product's stock movements
     * @throws com.example.smartinventory.exception.ResourceNotFoundException if the product does not
     *                                                                        exist
     */
    @Transactional(readOnly = true)
    public Page<StockMovement> findByProduct(Long productId, Pageable pageable) {
        productService.findById(productId);
        return stockMovementRepository.findByProductId(productId, pageable);
    }

}
