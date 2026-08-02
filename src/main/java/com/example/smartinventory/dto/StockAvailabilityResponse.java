package com.example.smartinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What a product's stock figure breaks down into: what is on hand, what of it is already spoken for,
 * and what is therefore left to promise.
 */
@Schema(description = "On hand, reserved and available stock for a product")
public record StockAvailabilityResponse(

        @Schema(description = "Identifier of the product", example = "1")
        Long productId,

        @Schema(description = "Identifier of the warehouse the figures are scoped to, or null for the "
                + "product total across every location", example = "1")
        Long warehouseId,

        @Schema(description = "Units physically held", example = "40")
        int onHand,

        @Schema(description = "Units of what is on hand that are already reserved", example = "12")
        int reserved,

        @Schema(description = "Units left to promise, never below zero", example = "28")
        int available) {

    /**
     * Builds the breakdown from what is held and what is reserved of it.
     *
     * <p>Available is floored at zero: stock can be shipped out from under a reservation, which
     * leaves more reserved than is on hand, and "minus four available" would only invite a caller to
     * treat the shortfall as headroom.
     *
     * @param productId   identifier of the product
     * @param warehouseId identifier of the warehouse the figures are scoped to, or {@code null}
     * @param onHand      units physically held
     * @param reserved    units of them already reserved
     * @return the availability breakdown
     */
    public static StockAvailabilityResponse of(Long productId, Long warehouseId, int onHand, int reserved) {
        return new StockAvailabilityResponse(productId, warehouseId, onHand, reserved,
                Math.max(0, onHand - reserved));
    }

}
