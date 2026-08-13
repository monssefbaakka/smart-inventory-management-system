package com.example.smartinventory.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.model.PurchaseOrderItem;
import com.example.smartinventory.model.PurchaseOrderStatus;
import com.example.smartinventory.model.StockLevel;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.PurchaseOrderRepository;
import com.example.smartinventory.repository.StockLevelRepository;

/**
 * Closes the loop between a low stock level and its replenishment: when a product falls to or
 * below its reorder threshold, a {@code DRAFT} purchase order is raised against its supplier so a
 * buyer only has to review and place it.
 *
 * <p>Which stock is measured depends on where the movement went through. A warehouse holding its own
 * reorder point for the product is measured against it, on its own quantity, and orders for itself;
 * anything else is measured against the product total, which is the only figure there was before
 * sites could name a reorder point of their own.
 *
 * <p>The rule is opt-in through {@code auto-reorder.enabled} and deliberately conservative. It
 * raises nothing for a product with no supplier, and nothing for a product that already sits on an
 * open order, so a shortfall that takes several movements to develop still results in one order.
 */
@Service
@Transactional
public class AutoReorderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoReorderService.class);

    /**
     * Orders that have not yet been received in full, and therefore already cover the shortfall. A
     * part-delivered order counts: the rest of it is still on its way, so a short delivery that
     * leaves the product below its threshold must not raise a second order for the same goods.
     */
    private static final List<PurchaseOrderStatus> OPEN_STATUSES = List.of(
            PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.PLACED, PurchaseOrderStatus.PARTIALLY_RECEIVED);

    /** Multiple of the reorder threshold a replenishment tops stock up to, absent an explicit quantity. */
    private static final int DEFAULT_TARGET_MULTIPLIER = 2;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockLevelRepository stockLevelRepository;
    private final boolean enabled;

    /**
     * Creates the reorder rule.
     *
     * @param purchaseOrderRepository store the raised orders are written to and checked against
     * @param stockLevelRepository    store the reorder point a warehouse holds for a product is read
     *                                from
     * @param enabled                 whether stock movements may raise orders at all
     */
    public AutoReorderService(PurchaseOrderRepository purchaseOrderRepository,
            StockLevelRepository stockLevelRepository,
            @Value("${auto-reorder.enabled:false}") boolean enabled) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.enabled = enabled;
    }

    /**
     * Raises a draft purchase order for the product when its overall stock has reached the reorder
     * threshold and nothing is on order for it yet.
     *
     * @param product the product whose stock level was just changed
     * @return the raised order, or {@code null} when the rule declined to raise one
     */
    public PurchaseOrder evaluate(Product product) {
        return evaluate(product, null);
    }

    /**
     * Raises a draft purchase order for the product when the stock the movement touched has reached
     * its reorder threshold and nothing is on order to cover it.
     *
     * <p>Called from within the stock movement transaction, so a failure to raise the order rolls
     * the movement back with it rather than leaving an order for stock that never moved.
     *
     * <p>A movement through a warehouse that holds its own reorder point for the product is measured
     * against that site alone and orders for it: sized to top that site back up, and delivered there
     * rather than wherever the supplier usually delivers. A total spread across four sites says
     * nothing about the one that has run out, and the site that has run out is the one that needs the
     * goods.
     *
     * <p>A movement through a warehouse that holds no reorder point, or through none at all, is
     * measured against the product total as before, and the order goes where the supplier's goods
     * normally go. Nobody is present to name a destination when a movement raises an order, so
     * without that the replenishment would arrive against the product total only — invisible to the
     * location that ran short in the first place.
     *
     * @param product   the product whose stock level was just changed
     * @param warehouse the location the stock moved through, or {@code null}
     * @return the raised order, or {@code null} when the rule declined to raise one
     */
    public PurchaseOrder evaluate(Product product, Warehouse warehouse) {
        if (!enabled) {
            return null;
        }

        StockLevel level = siteMeasuringItself(product, warehouse);
        return level == null ? evaluateProduct(product) : evaluateSite(product, warehouse, level);
    }

    /**
     * Reads off the level of the warehouse the movement went through, when that warehouse holds a
     * reorder point of its own for the product and is therefore measured on its own.
     *
     * @param product   the product that moved
     * @param warehouse the location it moved through, or {@code null}
     * @return that warehouse's level, or {@code null} when the product total answers for it
     */
    private StockLevel siteMeasuringItself(Product product, Warehouse warehouse) {
        if (warehouse == null) {
            return null;
        }
        return stockLevelRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .filter(level -> level.getReorderThreshold() != null)
                .orElse(null);
    }

    /**
     * Measures the product total against the threshold held on the product.
     *
     * @param product the product that moved
     * @return the raised order, or {@code null} when the rule declined to raise one
     */
    private PurchaseOrder evaluateProduct(Product product) {
        Integer quantity = product.getQuantity();
        Integer threshold = product.getReorderThreshold();
        if (quantity == null || threshold == null || quantity > threshold) {
            return null;
        }

        Supplier supplier = supplierOf(product);
        if (supplier == null) {
            return null;
        }

        if (purchaseOrderRepository.existsByStatusInAndItemsProductId(OPEN_STATUSES, product.getId())) {
            LOGGER.debug("Product {} is already on an open purchase order; not reordering", product.getId());
            return null;
        }

        return raise(product, supplier, supplier.getDefaultWarehouse(),
                replenishmentQuantity(product, quantity, threshold),
                "Automatic reorder: stock " + quantity + " at or below reorder threshold " + threshold);
    }

    /**
     * Measures one warehouse's stock against the reorder point that warehouse holds for the product,
     * and orders for that warehouse.
     *
     * @param product   the product that moved
     * @param warehouse the location it moved through
     * @param level     that location's level, which names a reorder point of its own
     * @return the raised order, or {@code null} when the rule declined to raise one
     */
    private PurchaseOrder evaluateSite(Product product, Warehouse warehouse, StockLevel level) {
        int quantity = level.getQuantity() == null ? 0 : level.getQuantity();
        int threshold = level.getReorderThreshold();
        if (quantity > threshold) {
            return null;
        }

        Supplier supplier = supplierOf(product);
        if (supplier == null) {
            return null;
        }

        if (purchaseOrderRepository.existsByStatusInAndWarehouseIdAndItemsProductId(
                OPEN_STATUSES, warehouse.getId(), product.getId())) {
            LOGGER.debug("Product {} is already on an open purchase order for warehouse {}; not reordering",
                    product.getId(), warehouse.getCode());
            return null;
        }

        return raise(product, supplier, warehouse, replenishmentQuantity(product, quantity, threshold),
                "Automatic reorder: stock " + quantity + " in " + warehouse.getCode()
                        + " at or below its reorder threshold " + threshold);
    }

    /**
     * Reads off who to order from, which a product does not have to have.
     *
     * @param product the product being replenished
     * @return the product's supplier, or {@code null} when it names none
     */
    private Supplier supplierOf(Product product) {
        Supplier supplier = product.getSupplier();
        if (supplier == null) {
            LOGGER.warn("Product {} is at or below its reorder threshold but has no supplier to order from",
                    product.getId());
        }
        return supplier;
    }

    /**
     * Writes the draft order a shortfall calls for.
     *
     * @param product   the product being replenished
     * @param supplier  who to order it from
     * @param warehouse where the goods are to be delivered, or {@code null}
     * @param quantity  units to order
     * @param note      what the order records about the shortfall that raised it
     * @return the persisted order
     */
    private PurchaseOrder raise(Product product, Supplier supplier, Warehouse warehouse, int quantity, String note) {
        PurchaseOrder order = PurchaseOrder.builder()
                .supplier(supplier)
                .warehouse(warehouse)
                .status(PurchaseOrderStatus.DRAFT)
                .autoGenerated(true)
                .note(note)
                .build();
        order.addItem(PurchaseOrderItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .build());

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        LOGGER.info("Raised automatic purchase order {} for {} units of product {} from supplier {}",
                saved.getId(), quantity, product.getId(), supplier.getId());
        return saved;
    }

    /**
     * Determines how much to order: the quantity configured on the product, or else enough to bring
     * the stock that was measured back to twice the threshold it was measured against. A threshold of
     * zero, or stock that somehow already exceeds that target, still orders a single unit rather than
     * an empty line.
     *
     * @param product   the product being replenished
     * @param onHand    units the measured stock currently holds
     * @param threshold the reorder point it fell to
     * @return the number of units to put on the order, always at least one
     */
    private int replenishmentQuantity(Product product, int onHand, int threshold) {
        Integer configured = product.getReorderQuantity();
        if (configured != null) {
            return configured;
        }
        return Math.max(threshold * DEFAULT_TARGET_MULTIPLIER - onHand, 1);
    }

}
