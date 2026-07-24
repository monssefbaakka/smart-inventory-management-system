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
        LOGGER.warn("Stock event {} for product {} (sku={}): quantity {} at or below threshold {}",
                notification.eventType(),
                notification.productId(),
                notification.sku(),
                notification.quantity(),
                notification.reorderThreshold());
    }

}
