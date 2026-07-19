package com.example.smartinventory.dto;

import com.example.smartinventory.model.MovementType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request payload for recording a stock movement. */
public record StockMovementRequest(

        @NotNull MovementType type,

        @NotNull @Positive Integer quantity,

        @Size(max = 500) String note) {
}
