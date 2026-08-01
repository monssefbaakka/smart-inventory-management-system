package com.example.smartinventory.model;

import java.time.Instant;

import org.hibernate.annotations.TenantId;

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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity recording a single stock quantity change for a {@link Product}. */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

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

    /** Location the movement applied to; absent for movements recorded without a warehouse. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    /** Lot the movement applied to; absent when it named none or was spread across several. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private ProductBatch batch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer quantity;

    @Size(max = 500)
    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

}
