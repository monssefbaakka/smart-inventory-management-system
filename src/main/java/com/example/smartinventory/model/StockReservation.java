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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity holding a quantity of a {@link Product} against an outbound commitment: stock that is
 * still on the shelf but is already spoken for, and therefore no longer available to promise to
 * anyone else.
 */
@Entity
@Table(name = "stock_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation {

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

    /** Location the stock is held in; absent when the hold is against the product total only. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    /** What the stock is held for, such as the sales order it was taken for. */
    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String reference;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.HELD;

    /** When the hold lapses; absent for a hold that stands until it is released or fulfilled. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Reports whether the hold has lapsed at the given moment. A reservation with no expiry never
     * lapses, and one that was already released or fulfilled has an outcome rather than an expiry.
     *
     * @param now the moment to judge the reservation at
     * @return {@code true} when the hold has lapsed
     */
    public boolean isExpiredAt(Instant now) {
        return status == ReservationStatus.HELD && expiresAt != null && !expiresAt.isAfter(now);
    }

    /**
     * Reports whether the reservation is still keeping stock off the available figure, which it does
     * while it is held and has not lapsed.
     *
     * @param now the moment to judge the reservation at
     * @return {@code true} when the reservation still holds its stock
     */
    public boolean holdsStockAt(Instant now) {
        return status == ReservationStatus.HELD && !isExpiredAt(now);
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = ReservationStatus.HELD;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}
