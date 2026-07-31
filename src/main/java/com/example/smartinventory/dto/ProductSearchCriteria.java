package com.example.smartinventory.dto;

import java.math.BigDecimal;

/**
 * The optional filters a product listing can be narrowed by. Every field is independent: those left
 * unset place no restriction, and those set are combined with AND.
 *
 * @param search     free text matched case-insensitively against the product name and SKU
 * @param categoryId keeps only products in the category with this identifier
 * @param supplierId keeps only products supplied by the supplier with this identifier
 * @param minPrice   keeps only products priced at or above this amount
 * @param maxPrice   keeps only products priced at or below this amount
 * @param lowStock   when true, keeps only products at or below their reorder threshold
 */
public record ProductSearchCriteria(
        String search,
        Long categoryId,
        Long supplierId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean lowStock) {

    /** Matches every product. */
    public static final ProductSearchCriteria UNFILTERED =
            new ProductSearchCriteria(null, null, null, null, null, false);

}
