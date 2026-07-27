package com.example.smartinventory.dto;

import com.example.smartinventory.model.MovementType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

        @NotNull @Positive
        @Schema(description = "Positive quantity moved (or absolute target for ADJUSTMENT)", example = "10")
        Integer quantity,

        @Size(max = 500)
        @Schema(description = "Optional free-text note", example = "Restock from supplier")
        String note) {
}
