package com.example.smartinventory.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Always-on channel that records stock events to the application log at WARN level. */
@Component
public class LoggingStockEventNotifier implements StockEventNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingStockEventNotifier.class);

    @Override
    public void send(StockEventNotification notification) {
        LOGGER.warn("Stock event {} for product {} (sku={}){}: {} free at or below threshold {}, "
                        + "with {} of the stock on hand reserved",
                notification.eventType(),
                notification.productId(),
                notification.sku(),
                location(notification),
                notification.quantity(),
                notification.reorderThreshold(),
                notification.reserved());
    }

    /**
     * Names the site the event belongs to, so the shelf that has to be filled is in the line itself
     * rather than in whatever the reader can work out from the product.
     *
     * @param notification the event being logged
     * @return the location clause, or an empty string for an event about the product as a whole
     */
    private String location(StockEventNotification notification) {
        return notification.warehouseCode() == null ? "" : " in warehouse " + notification.warehouseCode();
    }

}
