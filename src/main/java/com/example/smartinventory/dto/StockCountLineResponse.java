package com.example.smartinventory.dto;

import com.example.smartinventory.model.StockCountLine;

import io.swagger.v3.oas.annotations.media.Schema;

/** One product's counted quantity, alongside what was expected. */
@Schema(description = "A counted line on a stock count")
public record StockCountLineResponse(

        @Schema(description = "Identifier of the line", example = "9")
        Long id,

        @Schema(description = "Identifier of the counted product", example = "1")
        Long productId,

        @Schema(description = "Product SKU", example = "SKU-1")
        String sku,

        @Schema(description = "Product name", example = "Widget")
        String productName,

        @Schema(description = "Units found on the shelf", example = "38")
        Integer countedQuantity,

        @Schema(description = "Units the warehouse was believed to hold when the line was entered",
                example = "42")
        Integer expectedQuantity,

        @Schema(description = "Counted less expected; negative when stock is missing", example = "-4")
        Integer variance) {

    /**
     * Flattens a persisted line into its response form.
     *
     * @param line the line to convert; its product must be loadable
     * @return the response payload
     */
    public static StockCountLineResponse from(StockCountLine line) {
        return new StockCountLineResponse(
                line.getId(),
                line.getProduct().getId(),
                line.getProduct().getSku(),
                line.getProduct().getName(),
                line.getCountedQuantity(),
                line.getExpectedQuantity(),
                line.getVariance());
    }

}
