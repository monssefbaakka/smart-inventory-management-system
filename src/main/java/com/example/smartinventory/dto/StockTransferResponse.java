package com.example.smartinventory.dto;

import java.time.Instant;

import com.example.smartinventory.model.StockTransfer;

import io.swagger.v3.oas.annotations.media.Schema;

/** A completed move of stock from one warehouse to another. */
@Schema(description = "A recorded warehouse-to-warehouse stock transfer")
public record StockTransferResponse(

        @Schema(description = "Identifier of the transfer", example = "5")
        Long id,

        @Schema(description = "Identifier of the product moved", example = "1")
        Long productId,

        @Schema(description = "Product SKU", example = "SKU-1")
        String sku,

        @Schema(description = "Product name", example = "Widget")
        String productName,

        @Schema(description = "Identifier of the warehouse the stock left", example = "1")
        Long sourceWarehouseId,

        @Schema(description = "Code of the warehouse the stock left", example = "WH-NORTH")
        String sourceWarehouseCode,

        @Schema(description = "Identifier of the warehouse the stock arrived at", example = "2")
        Long destinationWarehouseId,

        @Schema(description = "Code of the warehouse the stock arrived at", example = "WH-SOUTH")
        String destinationWarehouseCode,

        @Schema(description = "Units moved", example = "10")
        Integer quantity,

        @Schema(description = "Free-text note recorded with the transfer", example = "Rebalancing")
        String note,

        @Schema(description = "When the transfer was recorded")
        Instant createdAt) {

    /**
     * Flattens a persisted transfer into its response form.
     *
     * @param transfer the transfer to convert; its product and both warehouses must be loadable
     * @return the response payload
     */
    public static StockTransferResponse from(StockTransfer transfer) {
        return new StockTransferResponse(
                transfer.getId(),
                transfer.getProduct().getId(),
                transfer.getProduct().getSku(),
                transfer.getProduct().getName(),
                transfer.getSourceWarehouse().getId(),
                transfer.getSourceWarehouse().getCode(),
                transfer.getDestinationWarehouse().getId(),
                transfer.getDestinationWarehouse().getCode(),
                transfer.getQuantity(),
                transfer.getNote(),
                transfer.getCreatedAt());
    }

}
