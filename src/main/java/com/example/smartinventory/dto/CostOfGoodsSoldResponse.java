package com.example.smartinventory.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/** What the stock that left over a window cost to acquire. */
@Schema(description = "Cost of the goods that left stock over a window")
public record CostOfGoodsSoldResponse(

        @Schema(description = "Start of the window, inclusive")
        Instant from,

        @Schema(description = "End of the window, exclusive")
        Instant to,

        @Schema(description = "Identifier of the product the window was narrowed to, when one was named",
                example = "1")
        Long productId,

        @Schema(description = "Units that left over the window", example = "120")
        Long quantity,

        @Schema(description = "What those units cost to acquire", example = "600.0000")
        BigDecimal totalCost) {

    /**
     * Builds the response from the aggregate of the window, reading the absence of any outward
     * movement as zero units worth nothing.
     *
     * @param from      start of the window, inclusive
     * @param to        end of the window, exclusive
     * @param productId the product the window was narrowed to, or {@code null}
     * @param totals    the summed movements of the window
     * @return the response payload
     */
    public static CostOfGoodsSoldResponse of(Instant from, Instant to, Long productId,
            CostOfGoodsSoldTotals totals) {
        Long quantity = totals == null || totals.quantity() == null ? 0L : totals.quantity();
        BigDecimal cost = totals == null || totals.cost() == null ? BigDecimal.ZERO : totals.cost();
        return new CostOfGoodsSoldResponse(from, to, productId, quantity, cost);
    }

}
