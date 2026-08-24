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
 * reorder point for the product is measured against it, on its own stock, and orders for itself;
 * anything else is measured against the product total, which is the only figure there was before
 * sites could name a reorder point of their own.
 *
 * <p>What is measured is free stock — what is on hand, less what reservations are holding. Buying
 * against the shelf rather than against what is left to sell starts the lead time after the last
 * unsold unit is gone, which is the one moment a reorder point exists to avoid.
 *
 * <p>What is on its way counts as cover. Stock bought and not yet delivered is added to the free
 * figure before it is compared with the reorder point, and the order is sized on the difference, so
 * an order for five against a shortfall of two hundred no longer silences the rule the way an order
 * for two hundred does. A shortfall that takes several movements to develop still results in one
 * order: the first order raised is outstanding, so the movement after it measures that cover and
 * declines.
 *
 * <p>What is ordered is a quantity the supplier can ship. A product naming a {@code packSize} has
 * the quantity the sizing arrived at rounded up to a whole number of packs, because an order for
 * seventeen of something sold in trays of twelve is a document a buyer has to correct before it can
 * be placed. Rounding decides how much, never whether: the comparison with the reorder point is made
 * before it and is not affected by it.
 *
 * <p>The rule is opt-in through {@code auto-reorder.enabled} and deliberately conservative. It
 * raises nothing for a product with no supplier, and nothing for stock whose shortfall is already
 * covered by what is on order.
 */
@Service
@Transactional
public class AutoReorderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoReorderService.class);

    /**
     * Orders that have not yet been received in full, and therefore still have goods to deliver. A
     * part-delivered order counts for what is left on it: the received part is already on the shelf
     * and counted there, and only the remainder is still on its way.
     */
    private static final List<PurchaseOrderStatus> OPEN_STATUSES = List.of(
            PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.PLACED, PurchaseOrderStatus.PARTIALLY_RECEIVED);

    /** Multiple of the reorder threshold a replenishment tops stock up to, absent an explicit quantity. */
    private static final int DEFAULT_TARGET_MULTIPLIER = 2;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockLevelRepository stockLevelRepository;
    private final AvailableStockService availableStockService;
    private final boolean enabled;

    /**
     * Creates the reorder rule.
     *
     * @param purchaseOrderRepository store the raised orders are written to and checked against
     * @param stockLevelRepository    store the reorder point a warehouse holds for a product is read
     *                                from
     * @param availableStockService   works out how much of the stock on hand is not already promised
     * @param enabled                 whether stock movements may raise orders at all
     */
    public AutoReorderService(PurchaseOrderRepository purchaseOrderRepository,
            StockLevelRepository stockLevelRepository,
            AvailableStockService availableStockService,
            @Value("${auto-reorder.enabled:false}") boolean enabled) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.availableStockService = availableStockService;
        this.enabled = enabled;
    }

    /**
     * Raises a draft purchase order for the product when its overall free stock, plus whatever is
     * already on its way, has reached the reorder threshold.
     *
     * @param product the product whose stock level was just changed
     * @return the raised order, or {@code null} when the rule declined to raise one
     */
    public PurchaseOrder evaluate(Product product) {
        return evaluate(product, null);
    }

    /**
     * Raises a draft purchase order for the product when the stock the movement touched has reached
     * its reorder threshold with too little on order to cover it.
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
     * Raises a draft purchase order when one warehouse has reached the reorder point it holds for the
     * product, without ever falling back to the product total.
     *
     * <p>For stock that only changed location. A transfer empties the site it leaves as surely as a
     * sale does, and the site has already said what empty means for it, so the source of a transfer is
     * measured against its own reorder point and orders for itself. What a transfer cannot do is move
     * the product total: the units it took out of one site went into another, and the group holds
     * exactly what it held before. So a warehouse naming no reorder point of its own raises nothing
     * here, rather than being measured against a figure the transfer left where it was.
     *
     * @param product   the product that moved
     * @param warehouse the location it moved out of
     * @return the raised order, or {@code null} when the rule declined to raise one
     */
    public PurchaseOrder evaluateRelocation(Product product, Warehouse warehouse) {
        if (!enabled) {
            return null;
        }

        StockLevel level = siteMeasuringItself(product, warehouse);
        return level == null ? null : evaluateSite(product, warehouse, level);
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
     * Measures the product total against the threshold held on the product: what is free anywhere,
     * plus what is outstanding on every open order for it, wherever that order is headed.
     *
     * @param product the product that moved
     * @return the raised order, or {@code null} when the rule declined to raise one
     */
    private PurchaseOrder evaluateProduct(Product product) {
        Integer threshold = product.getReorderThreshold();
        if (product.getQuantity() == null || threshold == null) {
            return null;
        }

        int free = availableStockService.measure(product).available();
        if (free > threshold) {
            return null;
        }

        Supplier supplier = supplierOf(product);
        if (supplier == null) {
            return null;
        }

        int incoming = (int) purchaseOrderRepository.sumOutstandingForProduct(OPEN_STATUSES, product.getId());
        int covered = free + incoming;
        if (covered > threshold) {
            LOGGER.debug("Product {} has {} units free and {} on order, above reorder threshold {}; not reordering",
                    product.getId(), free, incoming, threshold);
            return null;
        }

        int wanted = replenishmentQuantity(product, covered, threshold);
        int quantity = inWholePacks(product, wanted);
        return raise(product, supplier, supplier.getDefaultWarehouse(), quantity,
                "Automatic reorder: " + free + " units free" + onOrder(incoming)
                        + " at or below reorder threshold " + threshold
                        + roundedUp(product, wanted, quantity));
    }

    /**
     * Measures one warehouse's stock against the reorder point that warehouse holds for the product,
     * and orders for that warehouse. What is free at that site counts, and so does what is on its way
     * to it; an open order heading elsewhere fills another site's shelves and is not cover for this
     * one.
     *
     * @param product   the product that moved
     * @param warehouse the location it moved through
     * @param level     that location's level, which names a reorder point of its own
     * @return the raised order, or {@code null} when the rule declined to raise one
     */
    private PurchaseOrder evaluateSite(Product product, Warehouse warehouse, StockLevel level) {
        int free = availableStockService.measure(product, level).available();
        int threshold = level.getReorderThreshold();
        if (free > threshold) {
            return null;
        }

        Supplier supplier = supplierOf(product);
        if (supplier == null) {
            return null;
        }

        int incoming = (int) purchaseOrderRepository.sumOutstandingForProductInWarehouse(
                OPEN_STATUSES, warehouse.getId(), product.getId());
        int covered = free + incoming;
        if (covered > threshold) {
            LOGGER.debug("Product {} has {} units free in warehouse {} and {} on order for it, above its reorder "
                    + "threshold {}; not reordering", product.getId(), free, warehouse.getCode(), incoming, threshold);
            return null;
        }

        int wanted = replenishmentQuantity(product, covered, threshold);
        int quantity = inWholePacks(product, wanted);
        return raise(product, supplier, warehouse, quantity,
                "Automatic reorder: " + free + " units free in " + warehouse.getCode() + onOrder(incoming)
                        + " at or below its reorder threshold " + threshold
                        + roundedUp(product, wanted, quantity));
    }

    /**
     * Rounds a quantity up to a whole number of the packs the supplier ships, when the product says
     * what those are. Up rather than to nearest: rounding down would order less than the shortfall
     * just measured, and the next movement would raise a second order for the remainder — the
     * repetition the incoming-stock check exists to prevent.
     *
     * @param product  the product being replenished
     * @param quantity the number of units the sizing arrived at
     * @return that number rounded up to a whole pack, or unchanged when the product names no pack
     */
    private int inWholePacks(Product product, int quantity) {
        Integer packSize = product.getPackSize();
        if (packSize == null || packSize < 1) {
            return quantity;
        }
        return ((quantity + packSize - 1) / packSize) * packSize;
    }

    /**
     * Explains the rounding in the note the order carries, so a buyer reading a line for twenty-four
     * against a shortfall of seventeen can see where the extra seven came from. A quantity that was
     * already a whole number of packs, and a product that names no pack at all, say nothing.
     *
     * @param product  the product being replenished
     * @param wanted   the number of units the sizing arrived at
     * @param quantity the number actually being ordered
     * @return the rounding clause, or an empty string when nothing was rounded
     */
    private String roundedUp(Product product, int wanted, int quantity) {
        if (quantity == wanted) {
            return "";
        }
        int packSize = product.getPackSize();
        int packs = quantity / packSize;
        return "; " + wanted + " units rounded up to " + (packs == 1 ? "a pack" : packs + " packs")
                + " of " + packSize;
    }

    /**
     * Names what is already bought and still to arrive, for the note the order carries. A buyer
     * reading "two units free and five on order" can see why thirteen were ordered rather than
     * eighteen; a shortfall with nothing on its way says nothing about orders at all.
     *
     * @param incoming units outstanding on the open orders covering the measured stock
     * @return the on-order clause, or an empty string when nothing is coming
     */
    private String onOrder(int incoming) {
        return incoming == 0 ? "" : " and " + incoming + " on order";
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
     * the stock that was measured back to twice the threshold it was measured against, counting what
     * is already on its way as arrived. A threshold of zero, or cover that somehow already exceeds
     * that target, still orders a single unit rather than an empty line.
     *
     * <p>A configured {@code reorderQuantity} is ordered in full whatever is coming: it is a batch
     * size the buyer has chosen rather than a shortfall to be closed, and the rule has already
     * declined for anything the incoming stock covers.
     *
     * <p>Whatever this arrives at is then rounded up to a whole pack by {@link #inWholePacks}, a
     * configured batch included — a batch of fifty against a pack of twelve was never going to be
     * shipped as fifty, and forty-eight would leave it short.
     *
     * @param product   the product being replenished
     * @param covered   units the measured stock holds free, plus the units on their way to it
     * @param threshold the reorder point it fell to
     * @return the number of units to put on the order, always at least one
     */
    private int replenishmentQuantity(Product product, int covered, int threshold) {
        Integer configured = product.getReorderQuantity();
        if (configured != null) {
            return configured;
        }
        return Math.max(threshold * DEFAULT_TARGET_MULTIPLIER - covered, 1);
    }

}
