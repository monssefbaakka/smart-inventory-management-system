package com.example.smartinventory.dto;

import java.time.Instant;

import com.example.smartinventory.model.Supplier;

import io.swagger.v3.oas.annotations.media.Schema;

/** A supplier as returned by the API. */
@Schema(description = "A supplier of inventory products")
public record SupplierResponse(

        @Schema(description = "Identifier of the supplier", example = "1")
        Long id,

        @Schema(description = "Supplier name", example = "Acme Supplies")
        String name,

        @Schema(description = "Name of the contact person", example = "Jane Doe")
        String contactName,

        @Schema(description = "Contact email address", example = "sales@acme.test")
        String email,

        @Schema(description = "Contact phone number", example = "+1-555-0100")
        String phone,

        @Schema(description = "Postal address", example = "1 Industrial Way, Springfield")
        String address,

        @Schema(description = "When the supplier was created")
        Instant createdAt,

        @Schema(description = "When the supplier was last updated")
        Instant updatedAt) {

    /**
     * Flattens a persisted supplier into its response form. The supplier's products are
     * deliberately left out: they are a lazy association reachable through
     * {@code /api/products} instead.
     *
     * @param supplier the supplier to convert
     * @return the response payload
     */
    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getContactName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt());
    }

}
