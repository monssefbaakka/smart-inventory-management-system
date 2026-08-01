package com.example.smartinventory.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for declaring a batch (lot) of a product. */
@Schema(description = "A batch of a product to start tracking")
public record ProductBatchRequest(

        @NotBlank
        @Size(max = 64)
        @Schema(description = "Lot code printed on the goods, unique among the product's lots", example = "A-2291")
        String lotCode,

        @Schema(description = "Day the lot stops being sellable; omit for goods that do not expire",
                example = "2026-12-31")
        LocalDate expiryDate,

        @Schema(description = "Optional warehouse the lot is held in", example = "1")
        Long warehouseId) {
}
