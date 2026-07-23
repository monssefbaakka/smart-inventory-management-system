package com.example.smartinventory.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Request payload for a single purchase-order line item. */
@Schema(description = "A single line item on a purchase order")
public record PurchaseOrderItemRequest(

        @NotNull
        @Schema(description = "Identifier of the ordered product", example = "1")
        Long productId,

        @NotNull @Positive
        @Schema(description = "Quantity ordered", example = "50")
        Integer quantity,

        @NotNull @DecimalMin(value = "0.0", inclusive = true)
        @Schema(description = "Unit price at time of order", example = "9.99")
        BigDecimal unitPrice) {
}
