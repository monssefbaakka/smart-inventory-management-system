package com.example.smartinventory.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request payload for the quantity of one purchase-order line that arrived in a delivery, and where
 * it landed.
 *
 * <p>A line may be listed more than once in one receipt: that is how a delivery split across two
 * lots, or across two sites, is expressed. Repeated lines agreeing on both are taken together.
 */
@Schema(description = "The quantity of one ordered line that arrived, and where it was put away")
public record GoodsReceiptLineRequest(

        @NotNull
        @Schema(description = "Identifier of the purchase-order line item the goods belong to", example = "11")
        Long itemId,

        @NotNull @Positive
        @Schema(description = "Units that arrived, at most what is still outstanding on the line", example = "20")
        Integer quantity,

        @Schema(description = "Warehouse this line landed in, overriding the receipt's; omit to use the "
                + "receipt's warehouse", example = "2")
        Long warehouseId,

        @Size(max = 64)
        @Schema(description = "Lot code printed on the goods; the lot is created against the product if it does "
                + "not exist yet, and reused if it does", example = "A-2291")
        String lotCode,

        @Schema(description = "Day the lot stops being sellable; only meaningful alongside a lotCode",
                example = "2026-12-31")
        LocalDate expiryDate) {
}
