package com.example.smartinventory.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.smartinventory.model.Product;
import com.example.smartinventory.notification.StockEventNotification;
import com.example.smartinventory.notification.StockEventNotifier;
import com.example.smartinventory.notification.StockEventType;

import lombok.RequiredArgsConstructor;

/**
 * Evaluates a product's stock level and, when it has reached a low-stock or out-of-stock
 * condition, dispatches a {@link StockEventNotification} to every configured channel.
 */
@Service
@RequiredArgsConstructor
public class StockEventNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StockEventNotificationService.class);

    private final List<StockEventNotifier> notifiers;

    /**
     * Determines whether the product's current quantity warrants a notification and, if so,
     * delivers it to all channels. A channel that throws is logged and skipped so remaining
     * channels still receive the event.
     *
     * @param product the product whose stock level was just changed
     */
    public void evaluate(Product product) {
        StockEventType eventType = classify(product);
        if (eventType == null) {
            return;
        }

        StockEventNotification notification = new StockEventNotification(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getQuantity(),
                product.getReorderThreshold(),
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

    private StockEventType classify(Product product) {
        Integer quantity = product.getQuantity();
        Integer threshold = product.getReorderThreshold();
        if (quantity == null || threshold == null) {
            return null;
        }
        if (quantity <= 0) {
            return StockEventType.OUT_OF_STOCK;
        }
        if (quantity <= threshold) {
            return StockEventType.LOW_STOCK;
        }
        return null;
    }

}
