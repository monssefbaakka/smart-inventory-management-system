package com.example.smartinventory.dto;

import java.math.BigDecimal;

/**
 * Raw aggregate of the outward movements of a window, as the query returns it: both figures are
 * {@code null} when no stock left, because a sum over no rows is not zero, it is nothing.
 *
 * @param quantity units that left, or {@code null}
 * @param cost     what they were valued at, or {@code null}
 */
public record CostOfGoodsSoldTotals(Long quantity, BigDecimal cost) {
}
