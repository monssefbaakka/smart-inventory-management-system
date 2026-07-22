package com.example.smartinventory.service;

import java.math.BigDecimal;
import java.util.List;

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

import com.example.smartinventory.dto.PurchaseOrderItemRequest;
import com.example.smartinventory.dto.PurchaseOrderRequest;
import com.example.smartinventory.exception.InvalidPurchaseOrderStateException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.model.PurchaseOrderItem;
import com.example.smartinventory.model.PurchaseOrderStatus;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.repository.PurchaseOrderRepository;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierService supplierService;

    @Mock
    private ProductService productService;

    @Mock
    private StockMovementService stockMovementService;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    @Test
    void createBuildsDraftOrderWithResolvedItems() {
        Supplier supplier = Supplier.builder().id(7L).build();
        Product product = Product.builder().id(3L).build();
        when(supplierService.findById(7L)).thenReturn(supplier);
        when(productService.findById(3L)).thenReturn(product);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderRequest request = new PurchaseOrderRequest(7L, "urgent",
                List.of(new PurchaseOrderItemRequest(3L, 4, new BigDecimal("2.50"))));

        PurchaseOrder order = purchaseOrderService.create(request);

        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(order.getSupplier()).isSameAs(supplier);
        assertThat(order.getItems()).hasSize(1);
        PurchaseOrderItem item = order.getItems().get(0);
        assertThat(item.getProduct()).isSameAs(product);
        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(item.getPurchaseOrder()).isSameAs(order);
        assertThat(order.getTotal()).isEqualByComparingTo("10.00");
    }

    @Test
    void placeMovesDraftToPlaced() {
        PurchaseOrder order = PurchaseOrder.builder().id(1L).status(PurchaseOrderStatus.DRAFT).build();
        when(purchaseOrderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.place(1L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
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
        order.addItem(PurchaseOrderItem.builder().product(a).quantity(5).unitPrice(BigDecimal.ONE).build());
        order.addItem(PurchaseOrderItem.builder().product(b).quantity(2).unitPrice(BigDecimal.ONE).build());
        when(purchaseOrderRepository.findById(9L)).thenReturn(java.util.Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = purchaseOrderService.receive(9L);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        verify(stockMovementService).record(3L, MovementType.IN, 5, "Purchase order #9 received");
        verify(stockMovementService).record(4L, MovementType.IN, 2, "Purchase order #9 received");
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
        PurchaseOrder order = PurchaseOrder.builder().id(1L).build();
        when(purchaseOrderRepository.findBySupplierIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(order));

        List<PurchaseOrder> result = purchaseOrderService.findBySupplier(7L);

        assertThat(result).containsExactly(order);
        verify(supplierService).findById(7L);
    }

}
