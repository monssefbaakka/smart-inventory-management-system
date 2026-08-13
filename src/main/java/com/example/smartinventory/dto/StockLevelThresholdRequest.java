package com.example.smartinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

/** Request payload for setting how low one warehouse may get on one product before it reorders. */
@Schema(description = "A warehouse's own reorder point for a product")
public record StockLevelThresholdRequest(

        @PositiveOrZero
        @Schema(description = "Units this warehouse may fall to before it raises an order for "
                + "itself; null stops it being measured on its own", example = "5")
        Integer reorderThreshold) {
}
