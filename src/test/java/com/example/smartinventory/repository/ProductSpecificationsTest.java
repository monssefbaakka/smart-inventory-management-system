package com.example.smartinventory.repository;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import com.example.smartinventory.dto.ProductSearchCriteria;
import com.example.smartinventory.model.Product;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class ProductSpecificationsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Root<Product> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder builder;

    @Test
    void unsetCriteriaRestrictNothing() {
        Specification<Product> specification = ProductSpecifications.matching(ProductSearchCriteria.UNFILTERED);

        assertThat(specification.toPredicate(root, query, builder)).isNull();
        verifyNoInteractions(builder);
    }

    @Test
    void searchMatchesNameAndSkuCaseInsensitively() {
        toPredicate(criteria().search("WiD").build());

        ArgumentCaptor<String> pattern = ArgumentCaptor.captor();
        verify(builder, times(2)).like(any(), pattern.capture(), eq('\\'));
        assertThat(pattern.getAllValues()).containsExactly("%wid%", "%wid%");
        verify(builder).or(any(), any());
    }

    @Test
    void searchTreatsWildcardsInTheTermAsText() {
        toPredicate(criteria().search("100%_off").build());

        ArgumentCaptor<String> pattern = ArgumentCaptor.captor();
        verify(builder, times(2)).like(any(), pattern.capture(), eq('\\'));
        assertThat(pattern.getAllValues()).allMatch("%100\\%\\_off%"::equals);
    }

    @Test
    void blankSearchRestrictsNothing() {
        assertThat(toPredicate(criteria().search("   ").build())).isNull();
        verifyNoInteractions(builder);
    }

    @Test
    void categoryFilterComparesTheCategoryIdentifier() {
        toPredicate(criteria().categoryId(3L).build());

        verify(root.get("category")).get("id");
        verify(builder).equal(any(), eq(3L));
    }

    @Test
    void supplierFilterComparesTheSupplierIdentifier() {
        toPredicate(criteria().supplierId(4L).build());

        verify(root.get("supplier")).get("id");
        verify(builder).equal(any(), eq(4L));
    }

    @Test
    void minimumPriceKeepsProductsAtOrAboveIt() {
        toPredicate(criteria().minPrice(new BigDecimal("5.50")).build());

        verify(root).get("price");
        verify(builder).greaterThanOrEqualTo(any(), eq(new BigDecimal("5.50")));
    }

    @Test
    void maximumPriceKeepsProductsAtOrBelowIt() {
        toPredicate(criteria().maxPrice(new BigDecimal("50")).build());

        verify(root).get("price");
        verify(builder).lessThanOrEqualTo(any(), eq(new BigDecimal("50")));
    }

    @Test
    void lowStockComparesQuantityAgainstTheReorderThreshold() {
        toPredicate(criteria().lowStock(true).build());

        verify(root).get("quantity");
        verify(root).get("reorderThreshold");
        verify(builder).lessThanOrEqualTo(ArgumentMatchers.<Expression<Integer>>any(),
                ArgumentMatchers.<Expression<Integer>>any());
    }

    @Test
    void severalFiltersAreCombinedWithAnd() {
        toPredicate(criteria().categoryId(3L).minPrice(BigDecimal.ONE).lowStock(true).build());

        verify(builder, times(2)).and(any(Predicate.class), any(Predicate.class));
    }

    private Predicate toPredicate(ProductSearchCriteria criteria) {
        return ProductSpecifications.matching(criteria).toPredicate(root, query, builder);
    }

    private static CriteriaFixture criteria() {
        return new CriteriaFixture();
    }

    /** Assembles a {@link ProductSearchCriteria} one filter at a time. */
    private static final class CriteriaFixture {

        private String search;
        private Long categoryId;
        private Long supplierId;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private boolean lowStock;

        private CriteriaFixture search(String value) {
            this.search = value;
            return this;
        }

        private CriteriaFixture categoryId(Long value) {
            this.categoryId = value;
            return this;
        }

        private CriteriaFixture supplierId(Long value) {
            this.supplierId = value;
            return this;
        }

        private CriteriaFixture minPrice(BigDecimal value) {
            this.minPrice = value;
            return this;
        }

        private CriteriaFixture maxPrice(BigDecimal value) {
            this.maxPrice = value;
            return this;
        }

        private CriteriaFixture lowStock(boolean value) {
            this.lowStock = value;
            return this;
        }

        private ProductSearchCriteria build() {
            return new ProductSearchCriteria(search, categoryId, supplierId, minPrice, maxPrice, lowStock);
        }

    }

}
