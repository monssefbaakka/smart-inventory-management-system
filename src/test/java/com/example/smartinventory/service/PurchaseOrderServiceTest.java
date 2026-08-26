package com.example.smartinventory.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.smartinventory.dto.GoodsReceiptLineRequest;
import com.example.smartinventory.dto.GoodsReceiptRequest;
import com.example.smartinventory.dto.PurchaseOrderItemRequest;
import com.example.smartinventory.dto.PurchaseOrderRequest;
import com.example.smartinventory.dto.PurchaseOrderResponse;
import com.example.smartinventory.exception.InvalidBatchException;
import com.example.smartinventory.exception.InvalidPurchaseOrderStateException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.ProductBatch;
import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.model.PurchaseOrderItem;
import com.example.smartinventory.model.PurchaseOrderStatus;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.PurchaseOrderRepository;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    private static final Supplier SUPPLIER = Supplier.builder().id(7L).name("Acme Supplies").build();

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierService supplierService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private ProductService productService;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private ProductBatchService productBatchService;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    /** A placed order for five of product 3 on line 11 and two of product 4 on line 12. */
    private PurchaseOrder placedOrder() {
        PurchaseOrder order = PurchaseOrder.builder().id(9L).status(PurchaseOrderStatus.PLACED).build();
        order.addItem(PurchaseOrderItem.builder().id(11L).product(Product.builder().id(3L).build())
                .quantity(5).unitPrice(new BigDecimal("4.50")).build());
        order.addItem(PurchaseOrderItem.builder().id(12L).product(Product.builder().id(4L).build())
                .quantity(2).unitPrice(new BigDecimal("7.25")).build());
        return order;
    }

    /** The same order, raised to be delivered to warehouse 1. */
    private PurchaseOrder placedOrderDeliveredTo(Long warehouseId) {
        PurchaseOrder order = placedOrder();
        order.setWarehouse(Warehouse.builder().id(warehouseId).code("WH-NORTH").build());
        return order;
    }

    /** A draft order 1 raised against the given supplier, with nothing on it. */
    private PurchaseOrder draftFrom(Supplier supplier) {
        return PurchaseOrder.builder().id(1L).status(PurchaseOrderStatus.DRAFT).supplier(supplier).build();
    }

    /** A received line that says nothing about where the goods went. */
    private static GoodsReceiptLineRequest line(Long itemId, int quantity) {
        return new GoodsReceiptLineRequest(itemId, quantity, null, null, null);
    }

    /** A delivery that landed nowhere in particular. */
    private static GoodsReceiptRequest receipt(GoodsReceiptLineRequest... lines) {
        return new GoodsReceiptRequest(null, List.of(lines));
    }

    @Test
    void createBuildsDraftOrderWithResolvedItems() {
        Supplier supplier = Supplier.builder().id(7L).build();
        Product product = Product.builder().id(3L).build();
        when(supplierService.findById(7L)).thenReturn(supplier);
        when(productService.findById(3L)).thenReturn(product);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderRequest request = new PurchaseOrderRequest(7L, null, null, "urgent",
                List.of(new PurchaseOrderItemRequest(3L, 4, new BigDecimal("2.50"))));

        PurchaseOrder order = purchaseOrderService.create(request);

        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(order.getSupplier()).isSameAs(supplier);
        assertThat(order.getWarehouse()).isNull();
        assertThat(order.getItems()).hasSize(1);
        PurchaseOrderItem item = order.getItems().get(0);
        assertThat(item.getProduct()).isSameAs(product);
        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(item.getPurchaseOrder()).isSameAs(order);
        assertThat(order.getTotal()).isEqualByComparingTo("10.00");
        verifyNoInteractions(warehouseService);
    }

    @Test
    void createRecordsTheWarehouseTheOrderIsToBeDeliveredTo() {
        Warehouse warehouse = Warehouse.builder().id(1L).code("WH-NORTH").build();
        when(supplierService.findById(7L)).thenReturn(Supplier.builder().id(7L).build());
        when(warehouseService.findById(1L)).thenReturn(warehouse);
        when(productService.findById(3L)).thenReturn(Product.builder().id(3L).build());
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder order = purchaseOrderService.create(new PurchaseOrderRequest(7L, 1L, null, null,
                List.of(new PurchaseOrderItemRequest(3L, 4, new BigDecimal("2.50")))));

        assertThat(order.getWarehouse()).isSameAs(warehouse);
    }

    @Test
    void createDeliversToTheSupplierDefaultWarehouseWhenTheOrderNamesNone() {
        Warehouse defaultWarehouse = Warehouse.builder().id(2L).code("WH-SOUTH").build();
        when(supplierService.findById(7L))
                .thenReturn(Supplier.builder().id(7L).defaultWarehouse(defaultWarehouse).build());
        when(productService.findById(3L)).thenReturn(Product.builder().id(3L).build());
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder order = purchaseOrderService.create(new PurchaseOrderRequest(7L, null, null, null,
                List.of(new PurchaseOrderItemRequest(3L, 4, new BigDecimal("2.50")))));

        assertThat(order.getWarehouse()).isSameAs(defaultWarehouse);
        verifyNoInteractions(warehouseService);
    }

    @Test
    void createPrefersTheWarehouseTheOrderNamesOverTheSupplierDefault() {
        Warehouse named = Warehouse.builder().id(1L).code("WH-NORTH").build();
        when(supplierService.findById(7L)).thenReturn(Supplier.builder().id(7L)
                .defaultWarehouse(Warehouse.builder().id(2L).code("WH-SOUTH").build()).build());
        when(warehouseService.findById(1L)).thenReturn(named);
        when(productService.findById(3L)).thenReturn(Product.builder().id(3L).build());
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder order = purchaseOrderService.create(new PurchaseOrderRequest(7L, 1L, null, null,
                List.of(new PurchaseOrderItemRequest(3L, 4, new BigDecimal("2.50")))));

        assertThat(order.getWarehouse()).isSameAs(named);
    }

    @Test
    void createRejectsAWarehouseThatDoesNotExist() {
        when(supplierService.findById(7L)).thenReturn(Supplier.builder().id(7L).build());
        when(warehouseService.findById(99L)).thenThrow(new ResourceNotFoundException("Warehouse not found"));

        assertThatThrownBy(() -> purchaseOrderService.create(new PurchaseOrderRequest(7L, 99L, null, null,
                List.of(new PurchaseOrderItemRequest(3L, 4, new BigDecimal("2.50"))))))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(productService);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void createRecordsTheDeliveryDateTheBuyerNamed() {
        LocalDate promised = LocalDate.now().plusDays(5);
        when(supplierService.findById(7L)).thenReturn(Supplier.builder().id(7L).leadTimeDays(14).build());
        when(productService.findById(3L)).thenReturn(Product.builder().id(3L).build());
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder order = purchaseOrderService.create(new PurchaseOrderRequest(7L, null, promised, null,
                List.of(new PurchaseOrderItemRequest(3L, 4, new BigDecimal("2.50")))));

        assertThat(order.getExpectedDeliveryDate()).isEqualTo(promised);
    }

    @Test
    void createLeavesADraftOrderExpectingNothingUntilItIsPlaced() {
        when(supplierService.findById(7L)).thenReturn(Supplier.builder().id(7L).leadTimeDays(14).build());
        when(productService.findById(3L)).thenReturn(Product.builder().id(3L).build());
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder order = purchaseOrderService.create(new PurchaseOrderRequest(7L, null, null, null,
                List.of(new PurchaseOrderItemRequest(3L, 4, new BigDecimal("2.50")))));

        assertThat(order.getExpectedDeliveryDate()).isNull();
    }

    @Test
    void placeMovesDraftToPlaced() {
        PurchaseOrder order = draftFrom(SUPPLIER);
        when(purchaseOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.place(1L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
    }

    @Test
    void placeExpectsTheGoodsAfterTheSupplierLeadTime() {
        PurchaseOrder order = draftFrom(Supplier.builder().id(7L).leadTimeDays(14).build());
        when(purchaseOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.place(1L);

        assertThat(result.getExpectedDeliveryDate()).isEqualTo(LocalDate.now().plusDays(14));
    }

    @Test
    void placeKeepsTheDeliveryDateTheBuyerNamed() {
        LocalDate promised = LocalDate.now().plusDays(3);
        PurchaseOrder order = draftFrom(Supplier.builder().id(7L).leadTimeDays(14).build());
        order.setExpectedDeliveryDate(promised);
        when(purchaseOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.place(1L);

        assertThat(result.getExpectedDeliveryDate()).isEqualTo(promised);
    }

    @Test
    void placeExpectsNothingInParticularWhenTheSupplierNamesNoLeadTime() {
        PurchaseOrder order = draftFrom(SUPPLIER);
        when(purchaseOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.place(1L);

        assertThat(result.getExpectedDeliveryDate()).isNull();
    }

    @Test
    void placeRejectsNonDraftOrder() {
        PurchaseOrder order = PurchaseOrder.builder().id(1L).status(PurchaseOrderStatus.PLACED).build();
        when(purchaseOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> purchaseOrderService.place(1L))
                .isInstanceOf(InvalidPurchaseOrderStateException.class);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void receiveRecordsInMovementPerItemAndMarksReceived() {
        Product a = Product.builder().id(3L).build();
        Product b = Product.builder().id(4L).build();
        PurchaseOrder order = PurchaseOrder.builder().id(9L).status(PurchaseOrderStatus.PLACED).build();
        order.addItem(PurchaseOrderItem.builder().product(a).quantity(5).unitPrice(new BigDecimal("4.50")).build());
        order.addItem(PurchaseOrderItem.builder().product(b).quantity(2).unitPrice(new BigDecimal("7.25")).build());
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        verify(stockMovementService).record(3L, null, null, MovementType.IN, 5, "Purchase order #9 received",
                new BigDecimal("4.50"));
        verify(stockMovementService).record(4L, null, null, MovementType.IN, 2, "Purchase order #9 received",
                new BigDecimal("7.25"));
    }

    @Test
    void receivingEverythingStampsTheDayTheGoodsArrived() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(result.getDeliveredDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void receiveRejectsOrderThatIsNotPlaced() {
        PurchaseOrder order = PurchaseOrder.builder().id(1L).status(PurchaseOrderStatus.DRAFT).build();
        when(purchaseOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> purchaseOrderService.receive(1L))
                .isInstanceOf(InvalidPurchaseOrderStateException.class);
        verifyNoInteractions(stockMovementService);
    }

    @Test
    void deliveryBooksOnlyWhatArrivedAndLeavesTheOrderPartiallyReceived() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L, receipt(line(11L, 3)));

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        assertThat(result.getItems().get(0).getReceivedQuantity()).isEqualTo(3);
        assertThat(result.getItems().get(0).getOutstandingQuantity()).isEqualTo(2);
        assertThat(result.getItems().get(1).getReceivedQuantity()).isZero();
        verify(stockMovementService).record(3L, null, null, MovementType.IN, 3, "Purchase order #9 received",
                new BigDecimal("4.50"));
        verify(stockMovementService, never()).record(eq(4L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deliveryCompletingEveryLineMarksTheOrderReceived() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L, receipt(line(11L, 5), line(12L, 2)));

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(result.getItems()).allSatisfy(item -> assertThat(item.isFullyReceived()).isTrue());
    }

    @Test
    void aSecondDeliveryIsAcceptedAgainstAPartiallyReceivedOrder() {
        PurchaseOrder order = placedOrder();
        order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        order.getItems().get(0).setReceivedQuantity(3);
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L, receipt(line(11L, 2)));

        assertThat(result.getItems().get(0).getReceivedQuantity()).isEqualTo(5);
        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        verify(stockMovementService).record(3L, null, null, MovementType.IN, 2, "Purchase order #9 received",
                new BigDecimal("4.50"));
    }

    @Test
    void aDeliveryLeavingSomethingOutstandingStampsNoDayItArrived() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L, receipt(line(11L, 3)));

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        assertThat(result.getDeliveredDate()).isNull();
    }

    @Test
    void theDayAnOrderArrivedIsTheDayItsLastPartLanded() {
        PurchaseOrder order = placedOrder();
        order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        order.getItems().get(0).setReceivedQuantity(5);
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L, receipt(line(12L, 2)));

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(result.getDeliveredDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void deliveryBeyondWhatIsOutstandingBooksNothing() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> purchaseOrderService.receive(9L, receipt(line(11L, 5), line(12L, 3))))
                .isInstanceOf(InvalidPurchaseOrderStateException.class)
                .hasMessageContaining("only 2 outstanding");

        verifyNoInteractions(stockMovementService);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void deliveryAgainstALineOfAnotherOrderIsRejected() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> purchaseOrderService.receive(9L, receipt(line(99L, 1))))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(stockMovementService);
    }

    @Test
    void repeatedLinesInOneDeliveryAreTakenTogether() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L, receipt(line(11L, 2), line(11L, 3)));

        assertThat(result.getItems().get(0).getReceivedQuantity()).isEqualTo(5);
        verify(stockMovementService).record(3L, null, null, MovementType.IN, 5, "Purchase order #9 received",
                new BigDecimal("4.50"));
    }

    @Test
    void deliveryIsRejectedOnceTheOrderIsReceived() {
        PurchaseOrder order = PurchaseOrder.builder().id(9L).status(PurchaseOrderStatus.RECEIVED).build();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> purchaseOrderService.receive(9L, receipt(line(11L, 1))))
                .isInstanceOf(InvalidPurchaseOrderStateException.class);
    }

    @Test
    void deliveryLandsInTheWarehouseTheReceiptNames() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        purchaseOrderService.receive(9L, new GoodsReceiptRequest(1L, List.of(line(11L, 3), line(12L, 2))));

        verify(stockMovementService).record(3L, 1L, null, MovementType.IN, 3, "Purchase order #9 received",
                new BigDecimal("4.50"));
        verify(stockMovementService).record(4L, 1L, null, MovementType.IN, 2, "Purchase order #9 received",
                new BigDecimal("7.25"));
        verifyNoInteractions(productBatchService);
    }

    @Test
    void aLineLandsWhereItSaysRatherThanWhereTheRestOfTheDeliveryDid() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        purchaseOrderService.receive(9L, new GoodsReceiptRequest(1L, List.of(line(11L, 3),
                new GoodsReceiptLineRequest(12L, 2, 2L, null, null))));

        verify(stockMovementService).record(3L, 1L, null, MovementType.IN, 3, "Purchase order #9 received",
                new BigDecimal("4.50"));
        verify(stockMovementService).record(4L, 2L, null, MovementType.IN, 2, "Purchase order #9 received",
                new BigDecimal("7.25"));
    }

    @Test
    void aLotCodeOnALineBooksTheGoodsIntoThatLot() {
        PurchaseOrder order = placedOrder();
        LocalDate expiry = LocalDate.of(2026, 12, 31);
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productBatchService.findOrCreate(3L, 1L, "A-2291", expiry))
                .thenReturn(ProductBatch.builder().id(55L).build());

        purchaseOrderService.receive(9L, new GoodsReceiptRequest(1L,
                List.of(new GoodsReceiptLineRequest(11L, 5, null, "A-2291", expiry))));

        verify(stockMovementService).record(3L, 1L, 55L, MovementType.IN, 5, "Purchase order #9 received",
                new BigDecimal("4.50"));
    }

    @Test
    void oneLineSplitAcrossTwoLotsIsBookedIntoEachOfThem() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productBatchService.findOrCreate(3L, null, "A-2291", null))
                .thenReturn(ProductBatch.builder().id(55L).build());
        when(productBatchService.findOrCreate(3L, null, "A-2292", null))
                .thenReturn(ProductBatch.builder().id(56L).build());

        PurchaseOrder result = purchaseOrderService.receive(9L, receipt(
                new GoodsReceiptLineRequest(11L, 2, null, "A-2291", null),
                new GoodsReceiptLineRequest(11L, 3, null, "A-2292", null)));

        assertThat(result.getItems().get(0).getReceivedQuantity()).isEqualTo(5);
        verify(stockMovementService).record(3L, null, 55L, MovementType.IN, 2, "Purchase order #9 received",
                new BigDecimal("4.50"));
        verify(stockMovementService).record(3L, null, 56L, MovementType.IN, 3, "Purchase order #9 received",
                new BigDecimal("4.50"));
    }

    @Test
    void aSplitLineIsCappedByWhatTheWholeLineHasOutstanding() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> purchaseOrderService.receive(9L, receipt(
                new GoodsReceiptLineRequest(11L, 3, null, "A-2291", null),
                new GoodsReceiptLineRequest(11L, 3, null, "A-2292", null))))
                .isInstanceOf(InvalidPurchaseOrderStateException.class)
                .hasMessageContaining("only 5 outstanding");

        verifyNoInteractions(stockMovementService, productBatchService);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void anExpiryDateWithoutALotCodeIsRejected() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> purchaseOrderService.receive(9L, receipt(
                new GoodsReceiptLineRequest(11L, 3, null, null, LocalDate.of(2026, 12, 31)))))
                .isInstanceOf(InvalidBatchException.class)
                .hasMessageContaining("expiry date belongs to a lot");

        verifyNoInteractions(stockMovementService, productBatchService);
    }

    @Test
    void aDeliveryNamingNoWarehouseLandsWhereTheOrderWasToBeDeliveredTo() {
        PurchaseOrder order = placedOrderDeliveredTo(1L);
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        purchaseOrderService.receive(9L, receipt(line(11L, 3), line(12L, 2)));

        verify(stockMovementService).record(3L, 1L, null, MovementType.IN, 3, "Purchase order #9 received",
                new BigDecimal("4.50"));
        verify(stockMovementService).record(4L, 1L, null, MovementType.IN, 2, "Purchase order #9 received",
                new BigDecimal("7.25"));
    }

    @Test
    void theWarehouseOnTheReceiptBeatsTheOneTheOrderWasToBeDeliveredTo() {
        PurchaseOrder order = placedOrderDeliveredTo(1L);
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        purchaseOrderService.receive(9L, new GoodsReceiptRequest(2L, List.of(line(11L, 3),
                new GoodsReceiptLineRequest(12L, 2, 3L, null, null))));

        verify(stockMovementService).record(3L, 2L, null, MovementType.IN, 3, "Purchase order #9 received",
                new BigDecimal("4.50"));
        verify(stockMovementService).record(4L, 3L, null, MovementType.IN, 2, "Purchase order #9 received",
                new BigDecimal("7.25"));
    }

    @Test
    void aLineNamingItsOwnWarehouseBeatsTheOneTheOrderWasToBeDeliveredTo() {
        PurchaseOrder order = placedOrderDeliveredTo(1L);
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        purchaseOrderService.receive(9L, receipt(new GoodsReceiptLineRequest(11L, 3, 2L, null, null)));

        verify(stockMovementService).record(3L, 2L, null, MovementType.IN, 3, "Purchase order #9 received",
                new BigDecimal("4.50"));
    }

    @Test
    void aLotOnADeliveryIsHeldWhereTheOrderWasToBeDeliveredTo() {
        PurchaseOrder order = placedOrderDeliveredTo(1L);
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productBatchService.findOrCreate(3L, 1L, "A-2291", null))
                .thenReturn(ProductBatch.builder().id(55L).build());

        purchaseOrderService.receive(9L, receipt(new GoodsReceiptLineRequest(11L, 5, null, "A-2291", null)));

        verify(stockMovementService).record(3L, 1L, 55L, MovementType.IN, 5, "Purchase order #9 received",
                new BigDecimal("4.50"));
    }

    @Test
    void receiveInFullLandsWhereTheOrderWasToBeDeliveredTo() {
        PurchaseOrder order = placedOrderDeliveredTo(1L);
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        verify(stockMovementService).record(3L, 1L, null, MovementType.IN, 5, "Purchase order #9 received",
                new BigDecimal("4.50"));
        verify(stockMovementService).record(4L, 1L, null, MovementType.IN, 2, "Purchase order #9 received",
                new BigDecimal("7.25"));
    }

    @Test
    void receiveIntoAWarehouseOverridesTheOneTheOrderWasToBeDeliveredTo() {
        PurchaseOrder order = placedOrderDeliveredTo(1L);
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        purchaseOrderService.receive(9L, 2L);

        verify(stockMovementService).record(3L, 2L, null, MovementType.IN, 5, "Purchase order #9 received",
                new BigDecimal("4.50"));
        verify(stockMovementService).record(4L, 2L, null, MovementType.IN, 2, "Purchase order #9 received",
                new BigDecimal("7.25"));
    }

    @Test
    void receiveIntoAWarehouseBooksEveryOutstandingLineThere() {
        PurchaseOrder order = placedOrder();
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L, 1L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        verify(stockMovementService).record(3L, 1L, null, MovementType.IN, 5, "Purchase order #9 received",
                new BigDecimal("4.50"));
        verify(stockMovementService).record(4L, 1L, null, MovementType.IN, 2, "Purchase order #9 received",
                new BigDecimal("7.25"));
    }

    @Test
    void receiveClosesOutWhatIsStillOutstandingOnAPartiallyReceivedOrder() {
        PurchaseOrder order = placedOrder();
        order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        order.getItems().get(0).setReceivedQuantity(5);
        order.getItems().get(1).setReceivedQuantity(1);
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        verify(stockMovementService).record(4L, null, null, MovementType.IN, 1, "Purchase order #9 received",
                new BigDecimal("7.25"));
        verify(stockMovementService, never()).record(eq(3L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelAllowedFromAPartiallyReceivedOrder() {
        PurchaseOrder order = placedOrder();
        order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        order.getItems().get(0).setReceivedQuantity(3);
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.cancel(9L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
        assertThat(result.getItems().get(0).getReceivedQuantity()).isEqualTo(3);
        assertThat(result.getDeliveredDate()).isNull();
        verifyNoInteractions(stockMovementService);
    }

    @Test
    void cancelAllowedFromDraftAndPlaced() {
        PurchaseOrder placed = PurchaseOrder.builder().id(1L).status(PurchaseOrderStatus.PLACED).build();
        when(purchaseOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(placed));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.cancel(1L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    }

    @Test
    void cancelRejectsReceivedOrder() {
        PurchaseOrder order = PurchaseOrder.builder().id(1L).status(PurchaseOrderStatus.RECEIVED).build();
        when(purchaseOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> purchaseOrderService.cancel(1L))
                .isInstanceOf(InvalidPurchaseOrderStateException.class);
    }

    @Test
    void findBySupplierValidatesSupplierExists() {
        Pageable pageable = PageRequest.of(0, 20);
        PurchaseOrder order = PurchaseOrder.builder().id(1L).supplier(SUPPLIER).build();
        when(purchaseOrderRepository.findBySupplierId(7L, pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<PurchaseOrderResponse> result = purchaseOrderService.find(7L, false, pageable);

        assertThat(result.getContent()).singleElement()
                .satisfies(response -> assertThat(response.id()).isEqualTo(1L));
        verify(supplierService).findById(7L);
    }

    @Test
    void findWithoutASupplierReturnsAPageOfEveryOrder() {
        Pageable pageable = PageRequest.of(0, 20);
        PurchaseOrder order = PurchaseOrder.builder().id(1L).supplier(SUPPLIER).build();
        when(purchaseOrderRepository.findAllBy(pageable)).thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<PurchaseOrderResponse> result = purchaseOrderService.find(null, false, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verifyNoInteractions(supplierService);
    }

    @Test
    void findKeepsOnlyTheOrdersDueBeforeTodayWhenAskedForTheLateOnes() {
        Pageable pageable = PageRequest.of(0, 20);
        PurchaseOrder order = PurchaseOrder.builder().id(1L).supplier(SUPPLIER)
                .status(PurchaseOrderStatus.PLACED).expectedDeliveryDate(LocalDate.now().minusDays(1)).build();
        when(purchaseOrderRepository.findByStatusInAndExpectedDeliveryDateBefore(
                anyList(), eq(LocalDate.now()), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<PurchaseOrderResponse> result = purchaseOrderService.find(null, true, pageable);

        assertThat(result.getContent()).singleElement()
                .satisfies(response -> assertThat(response.overdue()).isTrue());

        ArgumentCaptor<List<PurchaseOrderStatus>> statuses = ArgumentCaptor.captor();
        verify(purchaseOrderRepository).findByStatusInAndExpectedDeliveryDateBefore(
                statuses.capture(), eq(LocalDate.now()), eq(pageable));
        assertThat(statuses.getValue())
                .containsExactlyInAnyOrder(PurchaseOrderStatus.PLACED, PurchaseOrderStatus.PARTIALLY_RECEIVED);
    }

    @Test
    void findAsksOneSupplierForItsLateOrders() {
        Pageable pageable = PageRequest.of(0, 20);
        when(purchaseOrderRepository.findBySupplierIdAndStatusInAndExpectedDeliveryDateBefore(
                eq(7L), anyList(), eq(LocalDate.now()), eq(pageable))).thenReturn(Page.empty(pageable));

        Page<PurchaseOrderResponse> result = purchaseOrderService.find(7L, true, pageable);

        assertThat(result).isEmpty();
        verify(supplierService).findById(7L);
        verify(purchaseOrderRepository, never()).findBySupplierId(any(), any());
    }

}
