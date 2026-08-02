package com.example.smartinventory.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.smartinventory.model.ReservationStatus;
import com.example.smartinventory.model.StockReservation;

/**
 * Repository for {@link StockReservation} persistence operations.
 *
 * <p>The finders fetch the lazy {@code product} and {@code warehouse} associations, because
 * responses are rendered from them after the transaction has closed.
 *
 * <p>The two sums answer "how much of this is already spoken for": only a reservation that is still
 * {@code HELD} and has not lapsed keeps its stock off the available figure.
 */
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    /** Only a held, unlapsed reservation counts against availability. */
    String STILL_HOLDING = "r.status = com.example.smartinventory.model.ReservationStatus.HELD "
            + "and (r.expiresAt is null or r.expiresAt > :now)";

    /** Newest first, so the most recent commitments read at the top of a listing. */
    String NEWEST_FIRST = "order by r.createdAt desc, r.id desc";

    @Override
    @EntityGraph(attributePaths = {"product", "warehouse"})
    Optional<StockReservation> findById(Long id);

    /**
     * Returns every reservation taken against a product, whatever became of it.
     *
     * @param productId identifier of the product
     * @return the product's reservations, newest first
     */
    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("select r from StockReservation r where r.product.id = :productId " + NEWEST_FIRST)
    List<StockReservation> findByProduct(@Param("productId") Long productId);

    /**
     * Returns the reservations of a product in one lifecycle status.
     *
     * @param productId identifier of the product
     * @param status    the status to report
     * @return the matching reservations, newest first
     */
    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("select r from StockReservation r where r.product.id = :productId and r.status = :status " + NEWEST_FIRST)
    List<StockReservation> findByProductAndStatus(@Param("productId") Long productId,
            @Param("status") ReservationStatus status);

    /**
     * Sums what is currently held against a product across every location, including the holds taken
     * without naming one.
     *
     * @param productId identifier of the product
     * @param now       the moment to judge the expiry times against
     * @return the units held, or zero when nothing is
     */
    @Query("select coalesce(sum(r.quantity), 0L) from StockReservation r where r.product.id = :productId "
            + "and " + STILL_HOLDING)
    long sumHeldForProduct(@Param("productId") Long productId, @Param("now") Instant now);

    /**
     * Sums what is currently held against a product in one warehouse. A hold that named no warehouse
     * is not counted here: it is a claim on the product total rather than on that location's shelf.
     *
     * @param productId   identifier of the product
     * @param warehouseId identifier of the warehouse
     * @param now         the moment to judge the expiry times against
     * @return the units held in that warehouse, or zero when nothing is
     */
    @Query("select coalesce(sum(r.quantity), 0L) from StockReservation r where r.product.id = :productId "
            + "and r.warehouse.id = :warehouseId and " + STILL_HOLDING)
    long sumHeldForProductInWarehouse(@Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId, @Param("now") Instant now);

}
