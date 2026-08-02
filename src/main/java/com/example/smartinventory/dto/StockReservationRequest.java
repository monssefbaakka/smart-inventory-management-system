package com.example.smartinventory.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request payload for holding stock of a product against an outbound commitment. */
@Schema(description = "Stock to hold against a commitment")
public record StockReservationRequest(

        @NotBlank
        @Size(max = 64)
        @Schema(description = "What the stock is held for, such as the sales order it was taken for",
                example = "SO-1042")
        String reference,

        @NotNull @Positive
        @Schema(description = "Units to hold", example = "12")
        Integer quantity,

        @Schema(description = "Optional warehouse to hold the stock in; omit to hold against the product "
                + "total only", example = "1")
        Long warehouseId,

        @Future
        @Schema(description = "Optional moment the hold lapses at; omit for a hold that stands until it is "
                + "released or fulfilled", example = "2026-08-09T17:00:00Z")
        Instant expiresAt) {
}
