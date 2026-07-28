package com.example.smartinventory.dto;

import java.time.Instant;
import java.util.List;

import com.example.smartinventory.model.StockCount;
import com.example.smartinventory.model.StockCountStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/** A stock count with its counted lines and their variances. */
@Schema(description = "A physical count of what one warehouse holds")
public record StockCountResponse(

        @Schema(description = "Identifier of the count", example = "3")
        Long id,

        @Schema(description = "Identifier of the counted warehouse", example = "1")
        Long warehouseId,

        @Schema(description = "Code of the counted warehouse", example = "WH-NORTH")
        String warehouseCode,

        @Schema(description = "Name of the counted warehouse", example = "Northern Depot")
        String warehouseName,

        @Schema(description = "Lifecycle status", example = "DRAFT")
        StockCountStatus status,

        @Schema(description = "Free-text note recorded with the count", example = "Quarterly cycle count")
        String note,

        @Schema(description = "The counted lines")
        List<StockCountLineResponse> lines,

        @Schema(description = "Net difference across every line", example = "-4")
        Integer totalVariance,

        @Schema(description = "When the count was opened")
        Instant createdAt,

        @Schema(description = "When the count was applied to stock; null until completed")
        Instant completedAt) {

    /**
     * Flattens a persisted count into its response form.
     *
     * @param count the count to convert; its warehouse, lines and their products must be loadable
     * @return the response payload
     */
    public static StockCountResponse from(StockCount count) {
        return new StockCountResponse(
                count.getId(),
                count.getWarehouse().getId(),
                count.getWarehouse().getCode(),
                count.getWarehouse().getName(),
                count.getStatus(),
                count.getNote(),
                count.getLines().stream().map(StockCountLineResponse::from).toList(),
                count.getTotalVariance(),
                count.getCreatedAt(),
                count.getCompletedAt());
    }

}
