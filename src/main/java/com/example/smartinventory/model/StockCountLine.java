package com.example.smartinventory.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity holding one product's counted quantity on a {@link StockCount}. */
@Entity
@Table(name = "stock_count_lines",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_count_lines_count_product",
                columnNames = {"stock_count_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCountLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCount stockCount;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Units actually found on the shelf. */
    @NotNull
    @PositiveOrZero
    @Column(name = "counted_quantity", nullable = false)
    private Integer countedQuantity;

    /**
     * What the warehouse was believed to hold when the line was entered, kept as a snapshot so the
     * variance stays meaningful even if the level moves before the count is completed.
     */
    @NotNull
    @PositiveOrZero
    @Column(name = "expected_quantity", nullable = false)
    private Integer expectedQuantity;

    /**
     * Difference between what was counted and what was expected: positive when the shelf held more
     * than the system believed, negative when stock was missing.
     *
     * @return the counted quantity less the expected quantity
     */
    public int getVariance() {
        return countedQuantity - expectedQuantity;
    }

}
