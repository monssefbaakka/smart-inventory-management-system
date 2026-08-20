package com.example.smartinventory.notification;

import java.time.Instant;

/**
 * Immutable payload describing a stock condition that crossed a notification threshold.
 *
 * <p>The condition belongs either to one location or to the product as a whole. When a warehouse
 * holds a reorder point of its own for the product, the quantity and threshold reported here are that
 * site's and the site is named; otherwise they are the product's overall figures and both warehouse
 * fields are {@code null}.
 *
 * <p>The quantity is free stock, not stock on the shelf: what is on hand less what reservations are
 * holding. A shelf of forty with thirty-eight promised reports two, with {@code reserved} saying why,
 * and on hand is the two figures added back together.
 *
 * @param productId        identifier of the affected product
 * @param sku              stock-keeping unit of the affected product
 * @param name             display name of the affected product
 * @param warehouseId      identifier of the location the condition belongs to, or {@code null} when
 *                         it belongs to the product as a whole
 * @param warehouseCode    code of that location, or {@code null}
 * @param quantity         free quantity that was measured: the site's when one is named, and the
 *                         product's overall free quantity otherwise
 * @param reserved         units of what is on hand that reservations are holding, in the same scope
 * @param reorderThreshold reorder threshold the quantity was measured against
 * @param eventType        kind of stock condition detected
 * @param occurredAt       instant the condition was detected
 */
public record StockEventNotification(
        Long productId,
        String sku,
        String name,
        Long warehouseId,
        String warehouseCode,
        Integer quantity,
        Integer reserved,
        Integer reorderThreshold,
        StockEventType eventType,
        Instant occurredAt) {

}
