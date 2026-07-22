package com.example.smartinventory.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request payload for creating a purchase order. */
public record PurchaseOrderRequest(

        @NotNull Long supplierId,

        @Size(max = 1000) String note,

        @NotEmpty @Valid List<PurchaseOrderItemRequest> items) {
}
