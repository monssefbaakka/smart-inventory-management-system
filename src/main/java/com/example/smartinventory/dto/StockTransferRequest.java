package com.example.smartinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request payload for moving stock from one warehouse to another. */
@Schema(description = "Details of a warehouse-to-warehouse stock transfer")
public record StockTransferRequest(

        @NotNull
        @Schema(description = "Identifier of the product being moved", example = "1")
        Long productId,

        @NotNull
        @Schema(description = "Identifier of the warehouse the stock leaves", example = "1")
        Long sourceWarehouseId,

        @NotNull
        @Schema(description = "Identifier of the warehouse the stock arrives at", example = "2")
        Long destinationWarehouseId,

        @NotNull @Positive
        @Schema(description = "Positive number of units to move", example = "10")
        Integer quantity,

        @Size(max = 500)
        @Schema(description = "Optional free-text note", example = "Rebalancing after regional demand spike")
        String note) {
}
