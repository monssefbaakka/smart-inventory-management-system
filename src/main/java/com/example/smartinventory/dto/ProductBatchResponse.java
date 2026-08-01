package com.example.smartinventory.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.example.smartinventory.model.ProductBatch;
import com.example.smartinventory.model.Warehouse;

import io.swagger.v3.oas.annotations.media.Schema;

/** A batch of a product as returned by the API, with its product and warehouse flattened. */
@Schema(description = "A tracked batch (lot) of a product")
public record ProductBatchResponse(

        @Schema(description = "Identifier of the batch", example = "5")
        Long id,

        @Schema(description = "Identifier of the product the lot belongs to", example = "1")
        Long productId,

        @Schema(description = "Product name", example = "Widget")
        String productName,

        @Schema(description = "Product stock keeping unit", example = "SKU-1")
        String productSku,

        @Schema(description = "Lot code printed on the goods", example = "A-2291")
        String lotCode,

        @Schema(description = "Day the lot stops being sellable, when it expires at all",
                example = "2026-12-31")
        LocalDate expiryDate,

        @Schema(description = "Units of this lot currently held", example = "40")
        Integer quantity,

        @Schema(description = "Identifier of the warehouse holding the lot, when one is set", example = "1")
        Long warehouseId,

        @Schema(description = "Code of the warehouse holding the lot, when one is set", example = "WH-1")
        String warehouseCode,

        @Schema(description = "Whether the lot is already past its expiry date", example = "false")
        boolean expired,

        @Schema(description = "When the lot started being tracked")
        Instant createdAt,

        @Schema(description = "When the lot was last changed")
        Instant updatedAt) {

    /**
     * Flattens a persisted batch into its response form.
     *
     * @param batch the batch to convert; its product and warehouse must be loadable
     * @return the response payload
     */
    public static ProductBatchResponse from(ProductBatch batch) {
        Warehouse warehouse = batch.getWarehouse();
        return new ProductBatchResponse(
                batch.getId(),
                batch.getProduct().getId(),
                batch.getProduct().getName(),
                batch.getProduct().getSku(),
                batch.getLotCode(),
                batch.getExpiryDate(),
                batch.getQuantity(),
                warehouse == null ? null : warehouse.getId(),
                warehouse == null ? null : warehouse.getCode(),
                batch.isExpiredOn(LocalDate.now()),
                batch.getCreatedAt(),
                batch.getUpdatedAt());
    }

}
