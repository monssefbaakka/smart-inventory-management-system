package com.example.smartinventory.dto;

import java.time.Instant;

import com.example.smartinventory.model.Category;

import io.swagger.v3.oas.annotations.media.Schema;

/** A product category as returned by the API. */
@Schema(description = "A product category")
public record CategoryResponse(

        @Schema(description = "Identifier of the category", example = "1")
        Long id,

        @Schema(description = "Category name", example = "Tools")
        String name,

        @Schema(description = "Free-text description", example = "Hand and power tools")
        String description,

        @Schema(description = "When the category was created")
        Instant createdAt,

        @Schema(description = "When the category was last updated")
        Instant updatedAt) {

    /**
     * Flattens a persisted category into its response form. The category's products are
     * deliberately left out: they are a lazy association reachable through
     * {@code /api/products} instead.
     *
     * @param category the category to convert
     * @return the response payload
     */
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

}
