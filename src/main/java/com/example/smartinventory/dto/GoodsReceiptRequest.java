package com.example.smartinventory.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * Request payload for booking a delivery against a purchase order. Only the lines that arrived are
 * listed; the lines left out are not received and stay outstanding.
 */
@Schema(description = "The lines of a purchase order that arrived in one delivery")
public record GoodsReceiptRequest(

        @Schema(description = "Warehouse the delivery landed in, applied to every line that does not name its "
                + "own; omit to book against the product total only", example = "1")
        Long warehouseId,

        @NotEmpty @Valid
        @Schema(description = "The received lines; at least one is required")
        List<GoodsReceiptLineRequest> lines) {
}
