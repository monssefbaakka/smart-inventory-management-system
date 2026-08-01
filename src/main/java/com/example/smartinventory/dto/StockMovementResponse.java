package com.example.smartinventory.dto;

import java.time.Instant;

import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.ProductBatch;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.model.Warehouse;

import io.swagger.v3.oas.annotations.media.Schema;

/** A single recorded change to a product's stock quantity. */
@Schema(description = "A recorded stock movement")
public record StockMovementResponse(

        @Schema(description = "Identifier of the movement", example = "7")
        Long id,

        @Schema(description = "Identifier of the product moved", example = "1")
        Long productId,

        @Schema(description = "Product SKU", example = "SKU-1")
        String sku,

        @Schema(description = "Product name", example = "Widget")
        String productName,

        @Schema(description = "Identifier of the warehouse the movement applied to, when one was named",
                example = "2")
        Long warehouseId,

        @Schema(description = "Code of the warehouse the movement applied to, when one was named",
                example = "WH-NORTH")
        String warehouseCode,

        @Schema(description = "Identifier of the batch the movement applied to, when one was named",
                example = "5")
        Long batchId,

        @Schema(description = "Lot code of the batch the movement applied to, when one was named",
                example = "A-2291")
        String lotCode,

        @Schema(description = "Direction of the movement", example = "IN")
        MovementType type,

        @Schema(description = "Units moved, or the new absolute quantity for an ADJUSTMENT", example = "10")
        Integer quantity,

        @Schema(description = "Free-text note recorded with the movement", example = "Delivery 42")
        String note,

        @Schema(description = "When the movement was recorded")
        Instant createdAt) {

    /**
     * Flattens a persisted movement into its response form.
     *
     * @param movement the movement to convert; its product and warehouse must be loadable
     * @return the response payload
     */
    public static StockMovementResponse from(StockMovement movement) {
        Warehouse warehouse = movement.getWarehouse();
        ProductBatch batch = movement.getBatch();
        return new StockMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getSku(),
                movement.getProduct().getName(),
                warehouse == null ? null : warehouse.getId(),
                warehouse == null ? null : warehouse.getCode(),
                batch == null ? null : batch.getId(),
                batch == null ? null : batch.getLotCode(),
                movement.getType(),
                movement.getQuantity(),
                movement.getNote(),
                movement.getCreatedAt());
    }

}
