package com.example.smartinventory.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request payload for creating a purchase order. */
@Schema(description = "Details for creating a purchase order against a supplier")
public record PurchaseOrderRequest(

        @NotNull
        @Schema(description = "Identifier of the supplier", example = "1")
        Long supplierId,

        @Size(max = 1000)
        @Schema(description = "Optional free-text note", example = "Q3 restock")
        String note,

        @NotEmpty @Valid
        @Schema(description = "Line items; at least one is required")
        List<PurchaseOrderItemRequest> items) {
}
