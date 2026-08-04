package com.example.smartinventory.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.smartinventory.dto.CostOfGoodsSoldTotals;
import com.example.smartinventory.model.StockMovement;

/**
 * Repository for {@link StockMovement} persistence operations.
 *
 * <p>The finders fetch the lazy {@code product}, {@code warehouse} and {@code batch} associations,
 * because responses are rendered from them after the transaction has closed.
 */
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * Returns one page of a product's movement history, ordered as {@code pageable} asks.
     *
     * @param productId identifier of the product
     * @param pageable  the page to return and the order to return it in
     * @return the requested page of that product's movements
     */
    @EntityGraph(attributePaths = {"product", "warehouse", "batch"})
    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    /**
     * Returns the most recent stock movements across all products, most recent first.
     *
     * @param pageable paging/limit information
     * @return the requested page of movements ordered by creation time descending
     */
    @EntityGraph(attributePaths = {"product", "warehouse", "batch"})
    List<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Sums the units and the cost of the stock that left over a window, across every product.
     *
     * <p>The movements recorded before stock was costed carry no cost and are summed as the units
     * they moved with no value against them, so an older history reports fewer money than goods.
     *
     * @param from start of the window, inclusive
     * @param to   end of the window, exclusive
     * @return the totals for the window; both figures are {@code null} when nothing left
     */
    @Query("""
            SELECT new com.example.smartinventory.dto.CostOfGoodsSoldTotals(SUM(m.quantity), SUM(m.totalCost))
            FROM StockMovement m
            WHERE m.type = com.example.smartinventory.model.MovementType.OUT
              AND m.createdAt >= :from AND m.createdAt < :to
            """)
    CostOfGoodsSoldTotals sumCostOfGoodsSold(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Sums the units and the cost of the stock that left over a window for one product.
     *
     * @param productId identifier of the product
     * @param from      start of the window, inclusive
     * @param to        end of the window, exclusive
     * @return the totals for the window; both figures are {@code null} when nothing left
     */
    @Query("""
            SELECT new com.example.smartinventory.dto.CostOfGoodsSoldTotals(SUM(m.quantity), SUM(m.totalCost))
            FROM StockMovement m
            WHERE m.type = com.example.smartinventory.model.MovementType.OUT
              AND m.product.id = :productId
              AND m.createdAt >= :from AND m.createdAt < :to
            """)
    CostOfGoodsSoldTotals sumCostOfGoodsSoldByProduct(@Param("productId") Long productId,
            @Param("from") Instant from, @Param("to") Instant to);

}
