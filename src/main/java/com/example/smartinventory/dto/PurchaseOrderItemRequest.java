package com.example.smartinventory.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Request payload for a single purchase-order line item. */
public record PurchaseOrderItemRequest(

        @NotNull Long productId,

        @NotNull @Positive Integer quantity,

        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal unitPrice) {
}
