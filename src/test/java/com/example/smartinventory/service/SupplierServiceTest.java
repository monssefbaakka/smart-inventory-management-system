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
