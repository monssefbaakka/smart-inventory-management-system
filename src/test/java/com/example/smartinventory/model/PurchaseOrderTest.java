package com.example.smartinventory.model;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PurchaseOrderTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 8);

    private PurchaseOrder order(PurchaseOrderStatus status, LocalDate due) {
        return PurchaseOrder.builder().id(1L).status(status).expectedDeliveryDate(due).build();
    }

    private PurchaseOrder delivered(LocalDate promisedFor, LocalDate arrivedOn) {
        return PurchaseOrder.builder().id(1L).status(PurchaseOrderStatus.RECEIVED)
                .expectedDeliveryDate(promisedFor).originalExpectedDeliveryDate(promisedFor)
                .deliveredDate(arrivedOn).build();
    }

    @Test
    void anOrderStillAwaitedIsLateOnceItsDateHasPassed() {
        assertThat(order(PurchaseOrderStatus.PLACED, TODAY.minusDays(1)).isOverdueOn(TODAY)).isTrue();
    }

    @Test
    void aPartDeliveredOrderIsLateForWhateverHasNotArrived() {
        assertThat(order(PurchaseOrderStatus.PARTIALLY_RECEIVED, TODAY.minusDays(1)).isOverdueOn(TODAY)).isTrue();
    }

    @Test
    void anOrderDueTodayHasTheWholeOfTheDay() {
        assertThat(order(PurchaseOrderStatus.PLACED, TODAY).isOverdueOn(TODAY)).isFalse();
    }

    @Test
    void anOrderDueLaterIsNotLate() {
        assertThat(order(PurchaseOrderStatus.PLACED, TODAY.plusDays(1)).isOverdueOn(TODAY)).isFalse();
    }

    @Test
    void aDeliveryIsJudgedByTheDaysBetweenThePromiseAndTheArrival() {
        assertThat(delivered(TODAY, TODAY.plusDays(3)).getDaysLate()).isEqualTo(3);
    }

    @Test
    void aDeliveryOnTheDayPromisedIsNoDaysLate() {
        assertThat(delivered(TODAY, TODAY).getDaysLate()).isZero();
    }

    @Test
    void anEarlyDeliveryIsReportedAsEarlyRatherThanAsOnTime() {
        assertThat(delivered(TODAY, TODAY.minusDays(2)).getDaysLate()).isEqualTo(-2);
    }

    @Test
    void aRePromisedDeliveryIsJudgedAgainstThePromiseItWasPlacedOn() {
        PurchaseOrder order = delivered(TODAY, TODAY.plusDays(14));
        order.setExpectedDeliveryDate(TODAY.plusDays(14));

        assertThat(order.getDaysLate()).isEqualTo(14);
    }

    @Test
    void anOrderCarryingFewerThanBothDatesIsNotJudged() {
        assertThat(delivered(TODAY, null).getDaysLate()).isNull();
        assertThat(delivered(null, TODAY).getDaysLate()).isNull();
    }

    @Test
    void anOrderNobodyPromisedADateForIsNeverLate() {
        assertThat(order(PurchaseOrderStatus.PLACED, null).isOverdueOn(TODAY)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class,
            names = {"DRAFT", "RECEIVED", "CANCELLED"})
    void nothingIsLateThatIsNotBeingWaitedOn(PurchaseOrderStatus status) {
        assertThat(order(status, TODAY.minusDays(30)).isOverdueOn(TODAY)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"PLACED", "PARTIALLY_RECEIVED"})
    void goodsAreStillExpectedAgainstAnOrderThatHasNotArrived(PurchaseOrderStatus status) {
        assertThat(status.isAwaitingDelivery()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"DRAFT", "RECEIVED", "CANCELLED"})
    void nobodyIsWaitingOnADraftOrOnAnOrderThatIsDone(PurchaseOrderStatus status) {
        assertThat(status.isAwaitingDelivery()).isFalse();
    }

}
