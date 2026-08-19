package com.example.smartinventory.notification;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.Test;

class LoggingStockEventNotifierTest {

    private final LoggingStockEventNotifier notifier = new LoggingStockEventNotifier();

    @Test
    void sendLogsWithoutThrowing() {
        StockEventNotification notification = new StockEventNotification(
                1L, "SKU-1", "Widget", null, null, 3, 10, StockEventType.LOW_STOCK, Instant.now());

        assertThatCode(() -> notifier.send(notification)).doesNotThrowAnyException();
    }

    @Test
    void sendLogsSiteEventWithoutThrowing() {
        StockEventNotification notification = new StockEventNotification(
                1L, "SKU-1", "Widget", 2L, "WH-NORTH", 3, 10, StockEventType.LOW_STOCK, Instant.now());

        assertThatCode(() -> notifier.send(notification)).doesNotThrowAnyException();
    }

}
