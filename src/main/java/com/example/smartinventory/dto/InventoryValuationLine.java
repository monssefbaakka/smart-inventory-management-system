package com.example.smartinventory.dto;

import java.math.BigDecimal;

import com.example.smartinventory.model.Product;

import io.swagger.v3.oas.annotations.media.Schema;

/** One product's contribution to the inventory value, at what its stock cost. */
@Schema(description = "A product's stock valued at cost")
public record InventoryValuationLine(

        @Schema(description = "Identifier of the product", example = "1")
        Long productId,

        @Schema(description = "Stock keeping unit", example = "SKU-1")
        String sku,

        @Schema(description = "Product name", example = "Widget")
        String name,

        @Schema(description = "Units on hand across all locations", example = "42")
        Integer quantity,

        @Schema(description = "Weighted average of what those units cost", example = "5.0000")
        BigDecimal averageCost,

        @Schema(description = "Quantity multiplied by the average cost", example = "210.0000")
        BigDecimal value) {

    /**
     * Values a product's stock at its average cost.
     *
     * @param product the product to value
     * @return the valuation line
     */
    public static InventoryValuationLine from(Product product) {
        BigDecimal averageCost = product.getAverageCost() == null ? BigDecimal.ZERO : product.getAverageCost();
        return new InventoryValuationLine(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getQuantity(),
                averageCost,
                averageCost.multiply(BigDecimal.valueOf(product.getQuantity())));
    }

}
