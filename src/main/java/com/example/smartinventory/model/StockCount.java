package com.example.smartinventory.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a physical count of what one {@link Warehouse} holds.
 *
 * <p>A count is entered as a set of {@link StockCountLine}s while it is {@code DRAFT}; nothing
 * reaches stock until the count is completed, at which point every line is applied as an adjustment
 * against the counted warehouse.
 */
@Entity
@Table(name = "stock_counts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning tenant; Hibernate stamps it on insert and filters every query by it. */
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    private String tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StockCountStatus status = StockCountStatus.DRAFT;

    @Size(max = 1000)
    @Column(length = 1000)
    private String note;

    @OneToMany(mappedBy = "stockCount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockCountLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** When the count was applied to stock; {@code null} until it is completed. */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Adds a counted line to this count and sets its back-reference.
     *
     * @param line the line to attach
     */
    public void addLine(StockCountLine line) {
        line.setStockCount(this);
        lines.add(line);
    }

    /**
     * Finds the line already raised for a product, if the product has been counted.
     *
     * @param productId identifier of the counted product
     * @return the existing line, or empty when the product has not been counted yet
     */
    public Optional<StockCountLine> findLineForProduct(Long productId) {
        return lines.stream()
                .filter(line -> line.getProduct().getId().equals(productId))
                .findFirst();
    }

    /**
     * Sums the variance across every counted line, so a count can be judged at a glance.
     *
     * @return the net difference between what was counted and what was expected
     */
    public int getTotalVariance() {
        return lines.stream().mapToInt(StockCountLine::getVariance).sum();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = StockCountStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}
