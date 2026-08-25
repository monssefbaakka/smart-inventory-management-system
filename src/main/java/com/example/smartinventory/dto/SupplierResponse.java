package com.example.smartinventory.dto;

import java.time.Instant;

import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.model.Warehouse;

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

        @Schema(description = "Identifier of the warehouse this supplier's goods are normally delivered to, or "
                + "null when it has no usual destination", example = "1")
        Long defaultWarehouseId,

        @Schema(description = "Code of the warehouse this supplier's goods are normally delivered to, or null "
                + "when it has no usual destination", example = "WH-NORTH")
        String defaultWarehouseCode,

        @Schema(description = "How many days pass between an order being placed with this supplier and the "
                + "goods arriving, or null when it is not known", example = "14")
        Integer leadTimeDays,

        @Schema(description = "When the supplier was created")
        Instant createdAt,

        @Schema(description = "When the supplier was last updated")
        Instant updatedAt) {

    /**
     * Flattens a persisted supplier into its response form. The supplier's products are
     * deliberately left out: they are a lazy association reachable through
     * {@code /api/products} instead.
     *
     * @param supplier the supplier to convert; its default delivery warehouse must be loadable
     * @return the response payload
     */
    public static SupplierResponse from(Supplier supplier) {
        Warehouse defaultWarehouse = supplier.getDefaultWarehouse();
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getContactName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                defaultWarehouse == null ? null : defaultWarehouse.getId(),
                defaultWarehouse == null ? null : defaultWarehouse.getCode(),
                supplier.getLeadTimeDays(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt());
    }

}
