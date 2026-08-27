package com.example.smartinventory.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.dto.SupplierReliabilityResponse;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.model.PurchaseOrderItem;
import com.example.smartinventory.model.PurchaseOrderStatus;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.PurchaseOrderRepository;
import com.example.smartinventory.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @InjectMocks
    private SupplierService supplierService;

    @Test
    void createSavesSupplier() {
        Supplier supplier = Supplier.builder().name("Acme").email("acme@example.com").build();
        when(supplierRepository.save(supplier)).thenReturn(supplier);

        Supplier result = supplierService.create(supplier);

        assertThat(result).isSameAs(supplier);
        assertThat(result.getDefaultWarehouse()).isNull();
        verifyNoInteractions(warehouseService);
    }

    @Test
    void createResolvesTheDefaultDeliveryWarehouse() {
        Warehouse persisted = Warehouse.builder().id(2L).code("WH-SOUTH").build();
        Supplier supplier = Supplier.builder().name("Acme").email("acme@example.com")
                .defaultWarehouse(Warehouse.builder().id(2L).build())
                .leadTimeDays(14)
                .build();
        when(warehouseService.findById(2L)).thenReturn(persisted);
        when(supplierRepository.save(supplier)).thenReturn(supplier);

        Supplier result = supplierService.create(supplier);

        assertThat(result.getDefaultWarehouse()).isSameAs(persisted);
        assertThat(result.getLeadTimeDays()).isEqualTo(14);
    }

    @Test
    void createRejectsADefaultWarehouseThatDoesNotExist() {
        Supplier supplier = Supplier.builder().name("Acme").email("acme@example.com")
                .defaultWarehouse(Warehouse.builder().id(99L).build())
                .build();
        when(warehouseService.findById(99L)).thenThrow(new ResourceNotFoundException("Warehouse not found"));

        assertThatThrownBy(() -> supplierService.create(supplier))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void findByIdReturnsSupplierWhenPresent() {
        Supplier supplier = Supplier.builder().id(1L).build();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        assertThat(supplierService.findById(1L)).isSameAs(supplier);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllReturnsAllSuppliers() {
        Supplier supplier = Supplier.builder().id(1L).build();
        when(supplierRepository.findAll()).thenReturn(List.of(supplier));

        assertThat(supplierService.findAll()).containsExactly(supplier);
    }

    @Test
    void updateAppliesFieldsAndSaves() {
        Warehouse persisted = Warehouse.builder().id(2L).code("WH-SOUTH").build();
        Supplier existing = Supplier.builder().id(1L).name("Old").email("old@example.com").build();
        Supplier updated = Supplier.builder()
                .name("New")
                .contactName("Jane")
                .email("new@example.com")
                .phone("123")
                .address("addr")
                .defaultWarehouse(Warehouse.builder().id(2L).build())
                .build();
        when(warehouseService.findById(2L)).thenReturn(persisted);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.update(1L, updated);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getContactName()).isEqualTo("Jane");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getPhone()).isEqualTo("123");
        assertThat(result.getAddress()).isEqualTo("addr");
        assertThat(result.getDefaultWarehouse()).isSameAs(persisted);
    }

    @Test
    void updateClearsTheDefaultWarehouseWhenThePayloadNamesNone() {
        Supplier existing = Supplier.builder().id(1L).name("Old").email("old@example.com")
                .defaultWarehouse(Warehouse.builder().id(2L).code("WH-SOUTH").build())
                .build();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.update(1L,
                Supplier.builder().name("Old").email("old@example.com").build());

        assertThat(result.getDefaultWarehouse()).isNull();
        verifyNoInteractions(warehouseService);
    }

    @Test
    void updateForgetsTheLeadTimeWhenThePayloadNamesNone() {
        Supplier existing = Supplier.builder().id(1L).name("Old").email("old@example.com").leadTimeDays(14).build();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.update(1L,
                Supplier.builder().name("Old").email("old@example.com").build());

        assertThat(result.getLeadTimeDays()).isNull();
    }

    /**
     * A delivery of one of this supplier's orders: promised for a day, arrived on another.
     *
     * @param promisedFor the day the goods were due
     * @param arrivedOn   the day they turned up
     * @return the fulfilled order
     */
    private PurchaseOrder delivery(LocalDate promisedFor, LocalDate arrivedOn) {
        return PurchaseOrder.builder()
                .status(PurchaseOrderStatus.RECEIVED)
                .expectedDeliveryDate(promisedFor)
                .originalExpectedDeliveryDate(promisedFor)
                .deliveredDate(arrivedOn)
                .build();
    }

    /**
     * A delivery that landed the stated number of days off its promise; negative is early.
     *
     * @param days days between the promise and the arrival
     * @return the fulfilled order
     */
    private PurchaseOrder deliveryLateBy(long days) {
        LocalDate promisedFor = LocalDate.of(2026, 9, 8);
        return delivery(promisedFor, promisedFor.plusDays(days));
    }

    /**
     * Gives a delivery a line worth the stated amount, so what it was worth can be summed.
     *
     * @param order  the order to price
     * @param amount what the delivery came to
     * @return the same order, now carrying a line
     */
    private PurchaseOrder worth(PurchaseOrder order, String amount) {
        order.addItem(PurchaseOrderItem.builder().quantity(1).unitPrice(new BigDecimal(amount)).build());
        return order;
    }

    /**
     * A delivery of one of that supplier's orders, landing the stated number of days off its promise.
     *
     * @param supplier the supplier who shipped it
     * @param days     days between the promise and the arrival; negative is early
     * @return the fulfilled order
     */
    private PurchaseOrder deliveryFrom(Supplier supplier, long days) {
        PurchaseOrder order = deliveryLateBy(days);
        order.setSupplier(supplier);
        return order;
    }

    @Test
    void reliabilityJudgesEveryDeliveryAgainstTheDayItWasPromised() {
        Supplier acme = Supplier.builder().id(7L).name("Acme Supplies").build();
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(acme));
        when(purchaseOrderRepository.findJudgeableDeliveries(7L, PurchaseOrderStatus.RECEIVED))
                .thenReturn(List.of(deliveryLateBy(0), deliveryLateBy(-2), deliveryLateBy(3), deliveryLateBy(11)));

        SupplierReliabilityResponse result = supplierService.reliability(7L);

        assertThat(result.supplierId()).isEqualTo(7L);
        assertThat(result.supplierName()).isEqualTo("Acme Supplies");
        assertThat(result.ordersJudged()).isEqualTo(4);
        assertThat(result.onTime()).isEqualTo(2);
        assertThat(result.late()).isEqualTo(2);
        assertThat(result.onTimeRate()).isEqualByComparingTo("0.50");
        assertThat(result.averageDaysLate()).isEqualByComparingTo("7.0");
        assertThat(result.worstDaysLate()).isEqualTo(11);
    }

    @Test
    void reliabilityAveragesTheLateDeliveriesOnlySoAnEarlyOneCannotCancelALateOne() {
        Supplier acme = Supplier.builder().id(7L).name("Acme Supplies").build();
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(acme));
        when(purchaseOrderRepository.findJudgeableDeliveries(7L, PurchaseOrderStatus.RECEIVED))
                .thenReturn(List.of(deliveryLateBy(-7), deliveryLateBy(7)));

        SupplierReliabilityResponse result = supplierService.reliability(7L);

        assertThat(result.averageDaysLate()).isEqualByComparingTo("7.0");
        assertThat(result.onTimeRate()).isEqualByComparingTo("0.50");
    }

    @Test
    void reliabilityReportsNothingRatherThanAPerfectRecordWhenNoDeliveryCanBeJudged() {
        Supplier acme = Supplier.builder().id(7L).name("Acme Supplies").build();
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(acme));
        when(purchaseOrderRepository.findJudgeableDeliveries(7L, PurchaseOrderStatus.RECEIVED))
                .thenReturn(List.of());

        SupplierReliabilityResponse result = supplierService.reliability(7L);

        assertThat(result.ordersJudged()).isZero();
        assertThat(result.onTime()).isZero();
        assertThat(result.late()).isZero();
        assertThat(result.onTimeRate()).isNull();
        assertThat(result.averageDaysLate()).isNull();
        assertThat(result.worstDaysLate()).isNull();
    }

    @Test
    void reliabilityLeavesTheAverageAndTheWorstUnstatedWhenNothingWasLate() {
        Supplier acme = Supplier.builder().id(7L).name("Acme Supplies").build();
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(acme));
        when(purchaseOrderRepository.findJudgeableDeliveries(7L, PurchaseOrderStatus.RECEIVED))
                .thenReturn(List.of(deliveryLateBy(0), deliveryLateBy(-3)));

        SupplierReliabilityResponse result = supplierService.reliability(7L);

        assertThat(result.onTime()).isEqualTo(2);
        assertThat(result.onTimeRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.averageDaysLate()).isNull();
        assertThat(result.worstDaysLate()).isNull();
    }

    @Test
    void theTableRanksTheWorstKeepersOfDatesFirstAndPutsTheUnjudgedLast() {
        Supplier acme = Supplier.builder().id(7L).name("Acme Supplies").build();
        Supplier bolt = Supplier.builder().id(8L).name("Bolt Brothers").build();
        Supplier cove = Supplier.builder().id(9L).name("Cove Trading").build();
        when(supplierRepository.findAll()).thenReturn(List.of(cove, acme, bolt));
        when(purchaseOrderRepository.findJudgeableDeliveries(PurchaseOrderStatus.RECEIVED)).thenReturn(List.of(
                deliveryFrom(acme, 4), deliveryFrom(acme, 0),
                deliveryFrom(bolt, 0), deliveryFrom(bolt, 0), deliveryFrom(bolt, 0), deliveryFrom(bolt, 2)));

        List<SupplierReliabilityResponse> table = supplierService.reliability();

        assertThat(table).extracting(SupplierReliabilityResponse::supplierName)
                .containsExactly("Acme Supplies", "Bolt Brothers", "Cove Trading");
        assertThat(table.get(0).onTimeRate()).isEqualByComparingTo("0.50");
        assertThat(table.get(1).onTimeRate()).isEqualByComparingTo("0.75");
        assertThat(table.get(2).ordersJudged()).isZero();
        assertThat(table.get(2).onTimeRate()).isNull();
    }

    @Test
    void theTableSettlesATieOnHowManyOrdersTheRowRestsOnAndThenOnName() {
        Supplier acme = Supplier.builder().id(7L).name("Acme Supplies").build();
        Supplier bolt = Supplier.builder().id(8L).name("Bolt Brothers").build();
        Supplier ashby = Supplier.builder().id(9L).name("Ashby Ltd").build();
        when(supplierRepository.findAll()).thenReturn(List.of(acme, bolt, ashby));
        when(purchaseOrderRepository.findJudgeableDeliveries(PurchaseOrderStatus.RECEIVED)).thenReturn(List.of(
                deliveryFrom(acme, 3),
                deliveryFrom(bolt, 3), deliveryFrom(bolt, 5),
                deliveryFrom(ashby, 3)));

        List<SupplierReliabilityResponse> table = supplierService.reliability();

        assertThat(table).extracting(SupplierReliabilityResponse::supplierName)
                .containsExactly("Bolt Brothers", "Acme Supplies", "Ashby Ltd");
    }

    @Test
    void theTableHoldsEverySupplierEvenWhereNobodyHasReceivedFromAnyOfThem() {
        Supplier acme = Supplier.builder().id(7L).name("Acme Supplies").build();
        when(supplierRepository.findAll()).thenReturn(List.of(acme));
        when(purchaseOrderRepository.findJudgeableDeliveries(PurchaseOrderStatus.RECEIVED)).thenReturn(List.of());

        List<SupplierReliabilityResponse> table = supplierService.reliability();

        assertThat(table).singleElement().satisfies(row -> {
            assertThat(row.supplierId()).isEqualTo(7L);
            assertThat(row.ordersJudged()).isZero();
            assertThat(row.onTimeRate()).isNull();
        });
    }

    @Test
    void reliabilitySumsWhatTheJudgedOrdersWereWorthAndWhatTheLateOnesCost() {
        Supplier acme = Supplier.builder().id(7L).name("Acme Supplies").build();
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(acme));
        when(purchaseOrderRepository.findJudgeableDeliveries(7L, PurchaseOrderStatus.RECEIVED)).thenReturn(List.of(
                worth(deliveryLateBy(0), "1000.00"),
                worth(deliveryLateBy(-2), "400.00"),
                worth(deliveryLateBy(3), "2500.50"),
                worth(deliveryLateBy(11), "99.50")));

        SupplierReliabilityResponse result = supplierService.reliability(7L);

        assertThat(result.judgedSpend()).isEqualByComparingTo("4000.00");
        assertThat(result.lateSpend()).isEqualByComparingTo("2600.00");
    }

    @Test
    void reliabilityReportsNoMoneyAsZeroWhereItReportsNoRateAsNull() {
        Supplier acme = Supplier.builder().id(7L).name("Acme Supplies").build();
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(acme));
        when(purchaseOrderRepository.findJudgeableDeliveries(7L, PurchaseOrderStatus.RECEIVED))
                .thenReturn(List.of());

        SupplierReliabilityResponse result = supplierService.reliability(7L);

        assertThat(result.judgedSpend()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.lateSpend()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.onTimeRate()).isNull();
    }

    @Test
    void reliabilityCountsNothingLateAsNoLateSpendWhileTheJudgedSpendStands() {
        Supplier acme = Supplier.builder().id(7L).name("Acme Supplies").build();
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(acme));
        when(purchaseOrderRepository.findJudgeableDeliveries(7L, PurchaseOrderStatus.RECEIVED)).thenReturn(List.of(
                worth(deliveryLateBy(0), "700.00"), worth(deliveryLateBy(-3), "300.00")));

        SupplierReliabilityResponse result = supplierService.reliability(7L);

        assertThat(result.judgedSpend()).isEqualByComparingTo("1000.00");
        assertThat(result.lateSpend()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void theTableCarriesTheMoneyBehindEachRow() {
        Supplier acme = Supplier.builder().id(7L).name("Acme Supplies").build();
        Supplier bolt = Supplier.builder().id(8L).name("Bolt Brothers").build();
        when(supplierRepository.findAll()).thenReturn(List.of(acme, bolt));
        when(purchaseOrderRepository.findJudgeableDeliveries(PurchaseOrderStatus.RECEIVED)).thenReturn(List.of(
                worth(deliveryFrom(acme, 4), "5000.00"), worth(deliveryFrom(acme, 0), "1000.00"),
                worth(deliveryFrom(bolt, 0), "20.00")));

        List<SupplierReliabilityResponse> table = supplierService.reliability();

        assertThat(table.get(0).supplierName()).isEqualTo("Acme Supplies");
        assertThat(table.get(0).judgedSpend()).isEqualByComparingTo("6000.00");
        assertThat(table.get(0).lateSpend()).isEqualByComparingTo("5000.00");
        assertThat(table.get(1).judgedSpend()).isEqualByComparingTo("20.00");
        assertThat(table.get(1).lateSpend()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void reliabilityRejectsASupplierThatDoesNotExist() {
        when(supplierRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.reliability(9L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(purchaseOrderRepository);
    }

    @Test
    void deleteRemovesExistingSupplier() {
        Supplier existing = Supplier.builder().id(1L).build();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));

        supplierService.delete(1L);

        verify(supplierRepository).delete(existing);
    }

}
