package com.example.smartinventory.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.model.Supplier;

import io.swagger.v3.oas.annotations.media.Schema;

/** How well a supplier has kept the dates their orders were promised for. */
@Schema(description = "A supplier's record of delivering when they said they would")
public record SupplierReliabilityResponse(

        @Schema(description = "Identifier of the supplier", example = "7")
        Long supplierId,

        @Schema(description = "Supplier name", example = "Acme Supplies")
        String supplierName,

        @Schema(description = "Delivered orders carrying both the day they were due and the day they arrived",
                example = "20")
        long ordersJudged,

        @Schema(description = "How many of them arrived on or before the day promised", example = "15")
        long onTime,

        @Schema(description = "How many of them arrived after it", example = "5")
        long late,

        @Schema(description = "Proportion of the judged orders that arrived on time, or null when none have "
                + "been judged", example = "0.75")
        BigDecimal onTimeRate,

        @Schema(description = "Mean lateness of the late orders in days, counting only those; null when none "
                + "of them was late", example = "4.2")
        BigDecimal averageDaysLate,

        @Schema(description = "Days late of the latest single order, or null when none of them was late",
                example = "11")
        Long worstDaysLate,

        @Schema(description = "What the judged orders were worth, at ordered quantity times unit price; zero "
                + "when none of them has been judged", example = "18400.00")
        BigDecimal judgedSpend,

        @Schema(description = "What the late ones among them were worth, on the same orders the record is "
                + "taken over; zero when none of them was late", example = "4600.00")
        BigDecimal lateSpend) {

    /** Scale the on-time proportion is reported at: a rate, not a sum of money. */
    private static final int RATE_SCALE = 2;

    /** Scale the average lateness is reported at; a tenth of a day is as fine as an average of days gets. */
    private static final int DAYS_SCALE = 1;

    /** Scale the sums of money are reported at, matching the scale a line's unit price is held at. */
    private static final int MONEY_SCALE = 2;

    /**
     * Folds a supplier's judged deliveries into their record.
     *
     * <p>A figure over no orders is {@code null} rather than zero. A supplier nobody has received
     * from yet has no record, which is neither a perfect one nor a terrible one, and reporting a
     * rate of zero for one and a rate of one for the other would be inventing both. The counts stay
     * zero, because none is a count.
     *
     * <p>The average is taken over the late orders only. Averaging the early deliveries in would let
     * one arriving a week early cancel one arriving a week late and report a supplier as punctual on
     * a record where nothing landed on the day it was promised; the warehouse that waited the week
     * did not experience an average.
     *
     * <p>The money is summed over exactly the orders the record is taken over, and a sum over none of
     * them is zero rather than null: no money went through, which is a fact, where no rate is not a
     * rate of nothing. A figure standing beside a rate has to rest on the same orders the rate does,
     * or the row invites reading one against the other and the reading is wrong.
     *
     * @param supplier   the supplier being judged
     * @param deliveries their fulfilled orders carrying both the day they were promised for and the
     *                   day they arrived
     * @return the response payload
     */
    public static SupplierReliabilityResponse of(Supplier supplier, List<PurchaseOrder> deliveries) {
        List<PurchaseOrder> late = deliveries.stream().filter(order -> order.getDaysLate() > 0).toList();
        long judged = deliveries.size();
        long onTime = judged - late.size();

        BigDecimal onTimeRate = judged == 0
                ? null
                : BigDecimal.valueOf(onTime).divide(BigDecimal.valueOf(judged), RATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal averageDaysLate = late.isEmpty()
                ? null
                : BigDecimal.valueOf(late.stream().mapToLong(PurchaseOrder::getDaysLate).sum())
                        .divide(BigDecimal.valueOf(late.size()), DAYS_SCALE, RoundingMode.HALF_UP);
        Long worstDaysLate = late.stream().map(PurchaseOrder::getDaysLate).max(Long::compareTo).orElse(null);

        return new SupplierReliabilityResponse(supplier.getId(), supplier.getName(), judged, onTime, late.size(),
                onTimeRate, averageDaysLate, worstDaysLate, spend(deliveries), spend(late));
    }

    /**
     * Sums what a set of orders was worth, at the ordered quantity times the unit price each line was
     * bought at, which is the total the order itself reports.
     *
     * @param orders the orders to add up
     * @return their total value, zero when there are none
     */
    private static BigDecimal spend(List<PurchaseOrder> orders) {
        return orders.stream()
                .map(PurchaseOrder::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

}
