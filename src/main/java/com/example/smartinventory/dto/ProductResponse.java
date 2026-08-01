package com.example.smartinventory.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.smartinventory.model.Category;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.Supplier;

import io.swagger.v3.oas.annotations.media.Schema;

/** A product as returned by the API, with its category and supplier flattened to id and name. */
@Schema(description = "An inventory product")
public record ProductResponse(

        @Schema(description = "Identifier of the product", example = "1")
        Long id,

        @Schema(description = "Product name", example = "Widget")
        String name,

        @Schema(description = "Stock keeping unit", example = "SKU-1")
        String sku,

        @Schema(description = "Scannable symbol content, when one is assigned", example = "5901234123457")
        String barcode,

        @Schema(description = "Free-text description", example = "A general purpose widget")
        String description,

        @Schema(description = "Unit price", example = "19.99")
        BigDecimal price,

        @Schema(description = "Units currently in stock across all locations", example = "42")
        Integer quantity,

        @Schema(description = "Quantity at or below which the product counts as low stock", example = "10")
        Integer reorderThreshold,

        @Schema(description = "Units to order when the product is replenished, when one is set", example = "50")
        Integer reorderQuantity,

        @Schema(description = "Identifier of the owning category, when one is set", example = "3")
        Long categoryId,

        @Schema(description = "Name of the owning category, when one is set", example = "Tools")
        String categoryName,

        @Schema(description = "Identifier of the supplying supplier, when one is set", example = "2")
        Long supplierId,

        @Schema(description = "Name of the supplying supplier, when one is set", example = "Acme Supplies")
        String supplierName,

        @Schema(description = "When the product was created")
        Instant createdAt,

        @Schema(description = "When the product was last updated")
        Instant updatedAt) {

    /**
     * Flattens a persisted product into its response form.
     *
     * @param product the product to convert; its category and supplier must be loadable
     * @return the response payload
     */
    public static ProductResponse from(Product product) {
        Category category = product.getCategory();
        Supplier supplier = product.getSupplier();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getReorderThreshold(),
                product.getReorderQuantity(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                supplier == null ? null : supplier.getId(),
                supplier == null ? null : supplier.getName(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

}
