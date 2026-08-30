package com.example.smartinventory.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

        @Schema(description = "What the judged orders were worth in total, at ordered quantity times unit "
                + "price; zero when none was judged", example = "48250.00")
        BigDecimal judgedSpend,

        @Schema(description = "How much of that arrived after the day it was promised for; zero when nothing "
                + "was late", example = "12100.00")
        BigDecimal lateSpend,

        @Schema(description = "Orders still being waited on whose promised day has passed, as of today",
                example = "3")
        long ordersOverdue,

        @Schema(description = "Days past its promise of the longest outstanding one, or null when none is "
                + "outstanding", example = "42")
        Long worstDaysOverdue,

        @Schema(description = "What those outstanding orders are worth, at ordered quantity times unit "
                + "price; zero when none is outstanding", example = "7400.00")
        BigDecimal overdueSpend) {

    /** Scale the on-time proportion is reported at: a rate, not a sum of money. */
    private static final int RATE_SCALE = 2;

    /** Scale the average lateness is reported at; a tenth of a day is as fine as an average of days gets. */
    private static final int DAYS_SCALE = 1;

    /**
     * Folds a supplier's judged deliveries into their record.
     *
     * <p>A rate over no orders is {@code null} rather than zero. A supplier nobody has received from
     * yet has no record, which is neither a perfect one nor a terrible one, and reporting a rate of
     * zero for one and a rate of one for the other would be inventing both. The counts stay zero,
     * because none is a count, and so do the sums: no money went through, which is a fact, where no
     * record is not a record of nothing.
     *
     * <p>The average is taken over the late orders only. Averaging the early deliveries in would let
     * one arriving a week early cancel one arriving a week late and report a supplier as punctual on
     * a record where nothing landed on the day it was promised; the warehouse that waited the week
     * did not experience an average.
     *
     * <p>The money is summed over exactly the orders the rates are taken over, so a sum standing
     * beside a rate can be read against it. Both are the order's own total, at ordered quantity
     * times unit price, which is the figure the order itself reports.
     *
     * <p>What is still owed is folded in beside what arrived. A supplier who delivers half their
     * orders punctually and sits on the rest is judged only on the half that came, and the worse
     * they are — never sending the goods rather than sending them late — the better the rates read.
     * An order that has not arrived cannot be on time or late, so it stays out of the rates and is
     * counted on its own terms: the figures stand beside each other and say different things.
     *
     * <p>The outstanding orders are read as of a given day rather than over the window the
     * deliveries are: what is owed is a fact about today, and an order overdue since before any
     * window is overdue now.
     *
     * @param supplier    the supplier being judged
     * @param deliveries  their fulfilled orders carrying both the day promised and the day of arrival
     * @param outstanding their orders still being waited on whose promised day has passed
     * @param on          the day the outstanding orders are measured against
     * @return the response payload
     */
    public static SupplierReliabilityResponse of(Supplier supplier, List<PurchaseOrder> deliveries,
            List<PurchaseOrder> outstanding, LocalDate on) {
        List<PurchaseOrder> late = deliveries.stream().filter(order -> order.getDaysLate() > 0).toList();
        List<Long> latenesses = late.stream().map(PurchaseOrder::getDaysLate).toList();
        long judged = deliveries.size();
        long onTime = judged - late.size();

        BigDecimal onTimeRate = judged == 0
                ? null
                : BigDecimal.valueOf(onTime).divide(BigDecimal.valueOf(judged), RATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal averageDaysLate = latenesses.isEmpty()
                ? null
                : BigDecimal.valueOf(latenesses.stream().mapToLong(Long::longValue).sum())
                        .divide(BigDecimal.valueOf(latenesses.size()), DAYS_SCALE, RoundingMode.HALF_UP);
        Long worstDaysLate = latenesses.stream().max(Long::compareTo).orElse(null);

        Long worstDaysOverdue = outstanding.stream()
                .map(order -> order.getDaysOverdueOn(on))
                .filter(days -> days != null)
                .max(Long::compareTo)
                .orElse(null);

        return new SupplierReliabilityResponse(supplier.getId(), supplier.getName(), judged, onTime, late.size(),
                onTimeRate, averageDaysLate, worstDaysLate, spendOver(deliveries), spendOver(late),
                outstanding.size(), worstDaysOverdue, spendOver(outstanding));
    }

    /**
     * Sums what a set of orders was worth, at ordered quantity times unit price.
     *
     * <p>Summed as the line values stand, with no conversion and no currency reported, because an
     * order line does not record one.
     *
     * @param orders the orders to total
     * @return their combined value, zero over none of them
     */
    private static BigDecimal spendOver(List<PurchaseOrder> orders) {
        return orders.stream().map(PurchaseOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
