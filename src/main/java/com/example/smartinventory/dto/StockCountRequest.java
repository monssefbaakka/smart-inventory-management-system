package com.example.smartinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request payload for opening a stock count. */
@Schema(description = "Details of a stock count to open against a warehouse")
public record StockCountRequest(

        @NotNull
        @Schema(description = "Identifier of the warehouse being counted", example = "1")
        Long warehouseId,

        @Size(max = 1000)
        @Schema(description = "Optional free-text note", example = "Quarterly cycle count, aisle A")
        String note) {
}
