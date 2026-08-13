package com.example.smartinventory.dto;

import com.example.smartinventory.model.StockLevel;

import io.swagger.v3.oas.annotations.media.Schema;

/** How much of one product is held in one warehouse. */
@Schema(description = "Quantity of a product held in a single warehouse")
public record StockLevelResponse(

        @Schema(description = "Identifier of the product", example = "1")
        Long productId,

        @Schema(description = "Product SKU", example = "SKU-1")
        String sku,

        @Schema(description = "Product name", example = "Widget")
        String productName,

        @Schema(description = "Identifier of the warehouse", example = "2")
        Long warehouseId,

        @Schema(description = "Warehouse code", example = "WH-NORTH")
        String warehouseCode,

        @Schema(description = "Warehouse name", example = "Northern Depot")
        String warehouseName,

        @Schema(description = "Units held in this warehouse", example = "42")
        Integer quantity,

        @Schema(description = "Reorder point this warehouse holds for this product, or null when it "
                + "is not measured on its own", example = "5")
        Integer reorderThreshold) {

    /**
     * Flattens a persisted level into its response form.
     *
     * @param level the level to convert; its product and warehouse must be loadable
     * @return the response payload
     */
    public static StockLevelResponse from(StockLevel level) {
        return new StockLevelResponse(
                level.getProduct().getId(),
                level.getProduct().getSku(),
                level.getProduct().getName(),
                level.getWarehouse().getId(),
                level.getWarehouse().getCode(),
                level.getWarehouse().getName(),
                level.getQuantity(),
                level.getReorderThreshold());
    }

}
