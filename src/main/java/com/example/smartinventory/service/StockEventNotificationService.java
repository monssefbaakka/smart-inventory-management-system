package com.example.smartinventory.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockLevel;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.notification.StockEventNotification;
import com.example.smartinventory.notification.StockEventNotifier;
import com.example.smartinventory.notification.StockEventType;
import com.example.smartinventory.repository.ProductRepository;
import com.example.smartinventory.repository.StockLevelRepository;

import lombok.RequiredArgsConstructor;

/**
 * Evaluates the stock a movement touched and, when it has reached a low-stock or out-of-stock
 * condition worth announcing, dispatches a {@link StockEventNotification} to every configured
 * channel.
 *
 * <p>Which stock is measured follows the reorder rule. A warehouse that holds its own reorder point
 * for the product is measured against it, on its own quantity, and the alert names that site;
 * anything else is measured against the product total, which is the only figure there was before
 * sites could name a reorder point of their own. A site is never measured both ways: one movement
 * says one thing about one shelf.
 *
 * <p>What is announced is the change, not the measurement. Each shelf remembers the condition it
 * last announced, and a measurement that repeats it dispatches nothing. An alert on every sale for a
 * shelf that has been low since Tuesday teaches its recipient to filter the folder, which is then
 * where the alert that matters lands.
 */
@Service
@RequiredArgsConstructor
public class StockEventNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StockEventNotificationService.class);

    private final List<StockEventNotifier> notifiers;
    private final ProductRepository productRepository;
    private final StockLevelRepository stockLevelRepository;

    /**
     * Determines whether the product's overall quantity warrants a notification and, if so, delivers
     * it to all channels.
     *
     * @param product the product whose stock level was just changed
     */
    public void evaluate(Product product) {
        evaluate(product, null);
    }

    /**
     * Determines whether the stock the movement touched warrants a notification and, if so, delivers
     * it to all channels. A channel that throws is logged and skipped so remaining channels still
     * receive the event.
     *
     * <p>A movement through a warehouse that holds its own reorder point for the product is judged
     * against that site alone, and the notification names it — a total spread across four sites says
     * nothing about the one whose shelf is empty, and it is that shelf the recipient has to go and
     * fill. A movement through a warehouse that holds no reorder point, or through none at all, is
     * judged against the product total as before and names no location.
     *
     * @param product   the product whose stock level was just changed
     * @param warehouse the location the stock moved through, or {@code null}
     */
    public void evaluate(Product product, Warehouse warehouse) {
        StockLevel level = siteMeasuringItself(product, warehouse);
        if (level == null) {
            measure(product, null, null, product.getQuantity(), product.getReorderThreshold());
        } else {
            measure(product, warehouse, level, level.getQuantity(), level.getReorderThreshold());
        }
    }

    /**
     * Determines whether one warehouse has reached the reorder point it holds for the product, without
     * ever falling back to the product total.
     *
     * <p>For stock that only changed location. A transfer empties the site it leaves as surely as a
     * sale does, and the site has already said what empty means for it. What a transfer cannot do is
     * move the product total, so a warehouse naming no reorder point of its own notifies nothing here
     * rather than being measured against a figure the transfer left where it was.
     *
     * @param product   the product that moved
     * @param warehouse the location it moved out of
     */
    public void evaluateRelocation(Product product, Warehouse warehouse) {
        StockLevel level = siteMeasuringItself(product, warehouse);
        if (level != null) {
            measure(product, warehouse, level, level.getQuantity(), level.getReorderThreshold());
        }
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
     * Classifies one quantity against one threshold, announces the result when it is worse than what
     * this shelf last announced, and records what now stands either way.
     *
     * <p>Four outcomes. A condition worse than the one standing — including the first one, which
     * stands against nothing — is dispatched and remembered. A condition that repeats the one
     * standing is neither dispatched nor written: it has already been said. A partial restock that
     * leaves the shelf still low lowers what stands without dispatching, so that a fall back to zero
     * is announced rather than swallowed. Stock back above the threshold clears what stands, arming
     * the shelf for its next fall; nothing is dispatched, because there is no channel here for good
     * news.
     *
     * @param product   the product whose stock was measured
     * @param warehouse the location the measured stock sits in, or {@code null} for the product total
     * @param level     that location's stock level, or {@code null} for the product total
     * @param quantity  the measured quantity
     * @param threshold the reorder point it was measured against
     */
    private void measure(Product product, Warehouse warehouse, StockLevel level, Integer quantity,
            Integer threshold) {
        if (quantity == null || threshold == null) {
            return;
        }

        StockEventType condition = classify(quantity, threshold);
        StockEventType standing = level == null ? product.getAnnouncedStockEvent() : level.getAnnouncedStockEvent();
        if (condition == standing) {
            return;
        }

        if (condition != null && condition.isWorseThan(standing)) {
            dispatch(product, warehouse, quantity, threshold, condition);
        }
        remember(product, level, condition);
    }

    /**
     * Delivers one condition to every channel. A channel that throws is logged and skipped so
     * remaining channels still receive the event.
     *
     * @param product   the product whose stock was measured
     * @param warehouse the location the measured stock sits in, or {@code null} for the product total
     * @param quantity  the measured quantity
     * @param threshold the reorder point it was measured against
     * @param eventType the condition to announce
     */
    private void dispatch(Product product, Warehouse warehouse, Integer quantity, Integer threshold,
            StockEventType eventType) {
        StockEventNotification notification = new StockEventNotification(
                product.getId(),
                product.getSku(),
                product.getName(),
                warehouse == null ? null : warehouse.getId(),
                warehouse == null ? null : warehouse.getCode(),
                quantity,
                threshold,
                eventType,
                Instant.now());

        for (StockEventNotifier notifier : notifiers) {
            try {
                notifier.send(notification);
            } catch (RuntimeException ex) {
                LOGGER.error("Stock event notifier {} failed for product {}",
                        notifier.getClass().getSimpleName(), product.getId(), ex);
            }
        }
    }

    /**
     * Records the condition that now stands for the shelf just measured, in the transaction that
     * moved the stock: a rolled-back movement remembers nothing, and a restart does not re-announce a
     * shortage that already stands. A channel that failed does not re-arm the shelf — a dead endpoint
     * is a channel problem, not a reason to say it all again to the channels that are up.
     *
     * @param product   the product that was measured
     * @param level     the location's stock level, or {@code null} when the product total was measured
     * @param condition the condition that now stands, or {@code null} when none does
     */
    private void remember(Product product, StockLevel level, StockEventType condition) {
        if (level == null) {
            product.setAnnouncedStockEvent(condition);
            productRepository.save(product);
        } else {
            level.setAnnouncedStockEvent(condition);
            stockLevelRepository.save(level);
        }
    }

    private StockEventType classify(Integer quantity, Integer threshold) {
        if (quantity <= 0) {
            return StockEventType.OUT_OF_STOCK;
        }
        if (quantity <= threshold) {
            return StockEventType.LOW_STOCK;
        }
        return null;
    }

}
