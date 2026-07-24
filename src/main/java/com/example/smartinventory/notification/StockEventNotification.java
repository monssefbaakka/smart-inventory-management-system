package com.example.smartinventory.notification;

import java.time.Instant;

/**
 * Immutable payload describing a stock condition that crossed a notification threshold.
 *
 * @param productId        identifier of the affected product
 * @param sku              stock-keeping unit of the affected product
 * @param name             display name of the affected product
 * @param quantity         product quantity after the triggering movement
 * @param reorderThreshold reorder threshold configured for the product
 * @param eventType        kind of stock condition detected
 * @param occurredAt       instant the condition was detected
 */
public record StockEventNotification(
        Long productId,
        String sku,
        String name,
        Integer quantity,
        Integer reorderThreshold,
        StockEventType eventType,
        Instant occurredAt) {

}
