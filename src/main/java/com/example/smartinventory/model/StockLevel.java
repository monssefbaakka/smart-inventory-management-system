package com.example.smartinventory.model;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity holding how much of a {@link Product} sits in one {@link Warehouse}. */
@Entity
@Table(name = "stock_levels",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_levels_product_warehouse",
                columnNames = {"product_id", "warehouse_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning tenant; Hibernate stamps it on insert and filters every query by it. */
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    private String tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    /**
     * How low this warehouse is allowed to get on this product before it orders for itself.
     * Optional: a level naming none is not measured on its own, and the product total answers for it
     * as it always has.
     */
    @PositiveOrZero
    @Column(name = "reorder_threshold")
    private Integer reorderThreshold;

    /**
     * The low-stock condition last announced to the notification channels for this warehouse's own
     * stock of this product. Held per level so one site falling quiet says nothing about another
     * site, or about the product total.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "announced_stock_event", length = 16)
    private StockEventType announcedStockEvent;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = Instant.now();
    }

}
