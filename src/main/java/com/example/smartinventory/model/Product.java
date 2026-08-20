package com.example.smartinventory.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.TenantId;

import com.example.smartinventory.notification.StockEventType;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity representing a product tracked in inventory. */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning tenant; Hibernate stamps it on insert and filters every query by it. */
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    private String tenantId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(unique = true, nullable = false)
    private String sku;

    /** Scannable symbol content (EAN/UPC/Code 128). Optional; unique when present. */
    @Size(max = 64)
    @Column(unique = true, length = 64)
    private String barcode;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Weighted average of what the units on hand cost to acquire. Never set from a request — it is
     * rolled forward by the receipts, so a product that has never been received at a stated cost
     * carries zero — and therefore never required of one either.
     */
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "average_cost", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal averageCost = BigDecimal.ZERO;

    @PositiveOrZero
    @Column(name = "reorder_threshold", nullable = false)
    @Builder.Default
    private Integer reorderThreshold = 10;

    /**
     * Units to order when the product is replenished. Optional: left unset, the automatic
     * reorder orders enough to bring stock back to twice the reorder threshold.
     */
    @Positive
    @Column(name = "reorder_quantity")
    private Integer reorderQuantity;

    /**
     * The low-stock condition last announced to the notification channels for the product total.
     * Null while nothing stands, which is what a comfortable product looks like and also what one
     * looks like before it has ever been announced.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "announced_stock_event", length = 16)
    private StockEventType announcedStockEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (reorderThreshold == null) {
            reorderThreshold = 10;
        }
        if (averageCost == null) {
            averageCost = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        if (averageCost == null) {
            averageCost = BigDecimal.ZERO;
        }
    }

}
