package com.example.smartinventory.dto;

import java.math.BigDecimal;

import com.example.smartinventory.model.MovementType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Request payload for recording a stock movement. */
@Schema(description = "Details of a stock movement to record against a product")
public record StockMovementRequest(

        @NotNull
        @Schema(description = "Movement direction", example = "IN")
        MovementType type,

        @Schema(description = "Optional warehouse the movement applies to; omit to move overall stock only",
                example = "1")
        Long warehouseId,

        @Schema(description = "Optional batch the movement applies to; an OUT movement naming none is "
                + "allocated across the product's batches earliest expiry first", example = "5")
        Long batchId,

        @NotNull @Positive
        @Schema(description = "Positive quantity moved (or absolute target for ADJUSTMENT)", example = "10")
        Integer quantity,

        @PositiveOrZero
        @Schema(description = "Cost of one received unit. Honoured on an IN movement, where it rolls the "
                + "product's weighted average cost; omit to value the movement at that average",
                example = "4.50")
        BigDecimal unitCost,

        @Size(max = 500)
        @Schema(description = "Optional free-text note", example = "Restock from supplier")
        String note) {
}
