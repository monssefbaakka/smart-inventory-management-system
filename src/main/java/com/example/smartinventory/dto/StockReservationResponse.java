package com.example.smartinventory.dto;

import java.time.Instant;

import com.example.smartinventory.model.ReservationStatus;
import com.example.smartinventory.model.StockReservation;
import com.example.smartinventory.model.Warehouse;

import io.swagger.v3.oas.annotations.media.Schema;

/** A stock reservation as returned by the API, with its product and warehouse flattened. */
@Schema(description = "Stock held against a commitment")
public record StockReservationResponse(

        @Schema(description = "Identifier of the reservation", example = "3")
        Long id,

        @Schema(description = "Identifier of the product held", example = "1")
        Long productId,

        @Schema(description = "Product name", example = "Widget")
        String productName,

        @Schema(description = "Product stock keeping unit", example = "SKU-1")
        String productSku,

        @Schema(description = "Identifier of the warehouse the stock is held in, when one is named",
                example = "1")
        Long warehouseId,

        @Schema(description = "Code of the warehouse the stock is held in, when one is named", example = "WH-1")
        String warehouseCode,

        @Schema(description = "What the stock is held for", example = "SO-1042")
        String reference,

        @Schema(description = "Units held", example = "12")
        Integer quantity,

        @Schema(description = "Lifecycle status of the reservation", example = "HELD")
        ReservationStatus status,

        @Schema(description = "Moment the hold lapses at, when it lapses at all",
                example = "2026-08-09T17:00:00Z")
        Instant expiresAt,

        @Schema(description = "Whether the hold has already lapsed and no longer holds its stock",
                example = "false")
        boolean expired,

        @Schema(description = "When the stock was reserved")
        Instant createdAt,

        @Schema(description = "When the reservation was last changed")
        Instant updatedAt) {

    /**
     * Flattens a persisted reservation into its response form.
     *
     * @param reservation the reservation to convert; its product and warehouse must be loadable
     * @return the response payload
     */
    public static StockReservationResponse from(StockReservation reservation) {
        Warehouse warehouse = reservation.getWarehouse();
        return new StockReservationResponse(
                reservation.getId(),
                reservation.getProduct().getId(),
                reservation.getProduct().getName(),
                reservation.getProduct().getSku(),
                warehouse == null ? null : warehouse.getId(),
                warehouse == null ? null : warehouse.getCode(),
                reservation.getReference(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getExpiresAt(),
                reservation.isExpiredAt(Instant.now()),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }

}
