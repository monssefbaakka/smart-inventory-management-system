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

        @NotEmpty @Valid
        @Schema(description = "The received lines; at least one is required")
        List<GoodsReceiptLineRequest> lines) {
}
