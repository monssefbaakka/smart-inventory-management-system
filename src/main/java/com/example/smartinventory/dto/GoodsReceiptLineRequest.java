package com.example.smartinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Request payload for the quantity of one purchase-order line that arrived in a delivery. */
@Schema(description = "The quantity of one ordered line that arrived")
public record GoodsReceiptLineRequest(

        @NotNull
        @Schema(description = "Identifier of the purchase-order line item the goods belong to", example = "11")
        Long itemId,

        @NotNull @Positive
        @Schema(description = "Units that arrived, at most what is still outstanding on the line", example = "20")
        Integer quantity) {
}
