package com.example.smartinventory.dto;

import java.math.BigDecimal;

import com.example.smartinventory.model.PurchaseOrderItem;

import io.swagger.v3.oas.annotations.media.Schema;

/** A single ordered line on a purchase order. */
@Schema(description = "A line item on a purchase order")
public record PurchaseOrderItemResponse(

        @Schema(description = "Identifier of the line item", example = "11")
        Long id,

        @Schema(description = "Identifier of the ordered product", example = "1")
        Long productId,

        @Schema(description = "Product SKU", example = "SKU-1")
        String sku,

        @Schema(description = "Product name", example = "Widget")
        String productName,

        @Schema(description = "Units ordered", example = "25")
        Integer quantity,

        @Schema(description = "Agreed price per unit", example = "9.50")
        BigDecimal unitPrice,

        @Schema(description = "Quantity multiplied by unit price", example = "237.50")
        BigDecimal lineTotal) {

    /**
     * Flattens a persisted line item into its response form.
     *
     * @param item the line item to convert; its product must be loadable
     * @return the response payload
     */
    public static PurchaseOrderItemResponse from(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

}
