package com.example.smartinventory.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The inventory valued at what it cost, product by product, with the total the lines add up to.
 *
 * @param products the products and what their stock is worth
 * @param total    the sum of the line values
 */
@Schema(description = "The inventory valued at cost")
public record InventoryValuationResponse(

        @Schema(description = "One line per product")
        List<InventoryValuationLine> products,

        @Schema(description = "Value of all stock on hand, at cost", example = "12480.5000")
        BigDecimal total) {
}
