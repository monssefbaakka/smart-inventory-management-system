package com.example.smartinventory.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.example.smartinventory.dto.ProductSearchCriteria;
import com.example.smartinventory.model.Product;

/** Builds the {@link Specification} that narrows a product listing to a set of search criteria. */
public final class ProductSpecifications {

    /** Escape character used so a literal {@code %} or {@code _} in a search term stays literal. */
    private static final char LIKE_ESCAPE = '\\';

    private ProductSpecifications() {
    }

    /**
     * Combines the filters carried by {@code criteria} with AND. Criteria that are unset contribute
     * nothing, so an entirely unset set of criteria matches every product.
     *
     * @param criteria the filters requested by the caller
     * @return the specification to run the listing query with
     */
    public static Specification<Product> matching(ProductSearchCriteria criteria) {
        List<Specification<Product>> filters = new ArrayList<>();
        if (criteria.search() != null && !criteria.search().isBlank()) {
            filters.add(nameOrSkuContains(criteria.search().strip()));
        }
        if (criteria.categoryId() != null) {
            filters.add(categoryIs(criteria.categoryId()));
        }
        if (criteria.supplierId() != null) {
            filters.add(supplierIs(criteria.supplierId()));
        }
        if (criteria.minPrice() != null) {
            filters.add(pricedAtLeast(criteria.minPrice()));
        }
        if (criteria.maxPrice() != null) {
            filters.add(pricedAtMost(criteria.maxPrice()));
        }
        if (criteria.lowStock()) {
            filters.add(atOrBelowReorderThreshold());
        }
        return Specification.allOf(filters);
    }

    private static Specification<Product> nameOrSkuContains(String term) {
        String pattern = "%" + escapeLikeWildcards(term.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), pattern, LIKE_ESCAPE),
                builder.like(builder.lower(root.get("sku")), pattern, LIKE_ESCAPE));
    }

    private static Specification<Product> categoryIs(Long categoryId) {
        return (root, query, builder) -> builder.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Product> supplierIs(Long supplierId) {
        return (root, query, builder) -> builder.equal(root.get("supplier").get("id"), supplierId);
    }

    private static Specification<Product> pricedAtLeast(BigDecimal minPrice) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.<BigDecimal>get("price"), minPrice);
    }

    private static Specification<Product> pricedAtMost(BigDecimal maxPrice) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.<BigDecimal>get("price"), maxPrice);
    }

    private static Specification<Product> atOrBelowReorderThreshold() {
        return (root, query, builder) ->
                builder.lessThanOrEqualTo(root.get("quantity"), root.<Integer>get("reorderThreshold"));
    }

    /**
     * Neutralises the wildcards a caller may have typed, so a search for {@code 100%} looks for that
     * text rather than for everything starting with {@code 100}.
     */
    private static String escapeLikeWildcards(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

}
