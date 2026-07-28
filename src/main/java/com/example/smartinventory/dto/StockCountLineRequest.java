package com.example.smartinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Request payload for recording one product's counted quantity on a stock count. */
@Schema(description = "A counted quantity for one product")
public record StockCountLineRequest(

        @NotNull
        @Schema(description = "Identifier of the counted product", example = "1")
        Long productId,

        @NotNull @PositiveOrZero
        @Schema(description = "Units found on the shelf; zero is a valid count", example = "38")
        Integer countedQuantity) {
}
