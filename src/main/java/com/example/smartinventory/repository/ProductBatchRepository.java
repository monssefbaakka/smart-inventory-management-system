package com.example.smartinventory.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.smartinventory.model.ProductBatch;

/**
 * Repository for {@link ProductBatch} persistence operations.
 *
 * <p>The finders fetch the lazy {@code product} and {@code warehouse} associations, because
 * responses are rendered from them after the transaction has closed.
 *
 * <p>Every listing orders by expiry date with the lots that never expire last, which is both the
 * order the expiry reports read best in and the order stock is allocated in.
 */
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    /** Earliest expiry first, lots without an expiry date last, oldest lot breaking a tie. */
    String EARLIEST_EXPIRY_FIRST = "order by case when b.expiryDate is null then 1 else 0 end, b.expiryDate, b.id";

    @Override
    @EntityGraph(attributePaths = {"product", "warehouse"})
    Optional<ProductBatch> findById(Long id);

    /**
     * Returns every lot of a product, whether or not it still holds stock.
     *
     * @param productId identifier of the product
     * @return the product's lots, earliest expiry first
     */
    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("select b from ProductBatch b where b.product.id = :productId " + EARLIEST_EXPIRY_FIRST)
    List<ProductBatch> findByProduct(@Param("productId") Long productId);

    /**
     * Reports whether a product already has a lot under this code, so the code stays unique among
     * the product's lots.
     *
     * @param productId identifier of the product
     * @param lotCode   the lot code to look for
     * @return {@code true} when the product already carries that lot code
     */
    boolean existsByProductIdAndLotCode(Long productId, String lotCode);

    /**
     * Returns the lot a product carries under a code, so goods arriving against a code already in
     * use are put into the lot they belong to rather than into a second one.
     *
     * @param productId identifier of the product
     * @param lotCode   the lot code to look for
     * @return the matching lot, if the product carries one
     */
    @EntityGraph(attributePaths = {"product", "warehouse"})
    Optional<ProductBatch> findByProductIdAndLotCode(Long productId, String lotCode);

    /**
     * Returns the lots of a product that still hold stock, in the order they should be consumed.
     *
     * @param productId identifier of the product
     * @return the allocatable lots, earliest expiry first
     */
    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("select b from ProductBatch b where b.product.id = :productId and b.quantity > 0 "
            + EARLIEST_EXPIRY_FIRST)
    List<ProductBatch> findAllocatable(@Param("productId") Long productId);

    /**
     * Returns the lots of a product that still hold stock in one warehouse, in the order they
     * should be consumed, so a movement out of a location never allocates stock held elsewhere.
     *
     * @param productId   identifier of the product
     * @param warehouseId identifier of the warehouse to allocate from
     * @return the allocatable lots in that warehouse, earliest expiry first
     */
    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("select b from ProductBatch b where b.product.id = :productId and b.quantity > 0 "
            + "and b.warehouse.id = :warehouseId " + EARLIEST_EXPIRY_FIRST)
    List<ProductBatch> findAllocatableInWarehouse(@Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    /**
     * Returns the lots still holding stock that expire between two days, both ends included, for
     * the "expiring soon" report.
     *
     * @param from earliest expiry date to report
     * @param to   latest expiry date to report
     * @return the matching lots, earliest expiry first
     */
    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("select b from ProductBatch b where b.quantity > 0 and b.expiryDate between :from and :to "
            + EARLIEST_EXPIRY_FIRST)
    List<ProductBatch> findExpiringBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Returns the lots that are past their expiry date but still hold stock, which is the stock
     * that has to be written off or quarantined.
     *
     * @param on the day to judge the lots against
     * @return the expired lots, earliest expiry first
     */
    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("select b from ProductBatch b where b.quantity > 0 and b.expiryDate < :on " + EARLIEST_EXPIRY_FIRST)
    List<ProductBatch> findExpiredOn(@Param("on") LocalDate on);

}
