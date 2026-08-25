package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.smartinventory.dto.GoodsReceiptRequest;
import com.example.smartinventory.dto.PurchaseOrderRequest;
import com.example.smartinventory.dto.PurchaseOrderResponse;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.model.PurchaseOrderItem;
import com.example.smartinventory.model.PurchaseOrderStatus;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.PurchaseOrderService;

@WebMvcTest(controllers = PurchaseOrderController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseOrderService purchaseOrderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private PurchaseOrder order(PurchaseOrderStatus status) {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L)
                .supplier(Supplier.builder().id(7L).name("Acme Supplies").build())
                .status(status)
                .build();
        order.addItem(PurchaseOrderItem.builder()
                .id(11L)
                .product(Product.builder().id(3L).sku("SKU-3").name("Widget").build())
                .quantity(4)
                .unitPrice(new BigDecimal("2.50"))
                .build());
        return order;
    }

    @Test
    void createReturnsCreatedDraft() throws Exception {
        when(purchaseOrderService.create(any(PurchaseOrderRequest.class))).thenReturn(order(PurchaseOrderStatus.DRAFT));

        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierId":7,"items":[{"productId":3,"quantity":4,"unitPrice":2.50}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.supplierId").value(7))
                .andExpect(jsonPath("$.supplierName").value("Acme Supplies"))
                .andExpect(jsonPath("$.warehouseId").value(nullValue()))
                .andExpect(jsonPath("$.warehouseCode").value(nullValue()))
                .andExpect(jsonPath("$.expectedDeliveryDate").value(nullValue()))
                .andExpect(jsonPath("$.total").value(10.00))
                .andExpect(jsonPath("$.items[0].productId").value(3))
                .andExpect(jsonPath("$.items[0].sku").value("SKU-3"))
                .andExpect(jsonPath("$.items[0].productName").value("Widget"))
                .andExpect(jsonPath("$.items[0].lineTotal").value(10.00))
                .andExpect(jsonPath("$.supplier").doesNotExist())
                .andExpect(jsonPath("$.items[0].product").doesNotExist());
    }

    @Test
    void createCarriesTheExpectedDeliveryDateThroughAndReportsIt() throws Exception {
        PurchaseOrder order = order(PurchaseOrderStatus.DRAFT);
        order.setExpectedDeliveryDate(LocalDate.of(2026, 9, 8));
        when(purchaseOrderService.create(any(PurchaseOrderRequest.class))).thenReturn(order);

        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierId":7,"expectedDeliveryDate":"2026-09-08",
                                 "items":[{"productId":3,"quantity":4,"unitPrice":2.50}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expectedDeliveryDate").value("2026-09-08"));

        ArgumentCaptor<PurchaseOrderRequest> request = ArgumentCaptor.captor();
        verify(purchaseOrderService).create(request.capture());
        assertThat(request.getValue().expectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 9, 8));
    }

    @Test
    void createCarriesTheDeliveryWarehouseThroughAndReportsIt() throws Exception {
        PurchaseOrder order = order(PurchaseOrderStatus.DRAFT);
        order.setWarehouse(Warehouse.builder().id(2L).code("WH-NORTH").build());
        when(purchaseOrderService.create(any(PurchaseOrderRequest.class))).thenReturn(order);

        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierId":7,"warehouseId":2,
                                 "items":[{"productId":3,"quantity":4,"unitPrice":2.50}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warehouseId").value(2))
                .andExpect(jsonPath("$.warehouseCode").value("WH-NORTH"));

        ArgumentCaptor<PurchaseOrderRequest> request = ArgumentCaptor.captor();
        verify(purchaseOrderService).create(request.capture());
        assertThat(request.getValue().warehouseId()).isEqualTo(2L);
    }

    @Test
    void createRejectsOrderWithNoItems() throws Exception {
        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierId":7,"items":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAllReturnsAPageOfOrders() throws Exception {
        when(purchaseOrderService.find(isNull(), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(PurchaseOrderResponse.from(order(PurchaseOrderStatus.DRAFT))),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void findAllFiltersBySupplierWhenProvided() throws Exception {
        when(purchaseOrderService.find(eq(7L), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(PurchaseOrderResponse.from(order(PurchaseOrderStatus.PLACED))),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/purchase-orders").param("supplierId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PLACED"));

        verify(purchaseOrderService).find(eq(7L), any(Pageable.class));
    }

    @Test
    void findAllDefaultsToTheMostRecentFirst() throws Exception {
        when(purchaseOrderService.find(isNull(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(purchaseOrderService).find(isNull(), pageable.capture());
        assertThat(pageable.getValue())
                .isEqualTo(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void findAllHonoursThePagingAndSortingAsked() throws Exception {
        when(purchaseOrderService.find(isNull(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/purchase-orders").param("page", "1").param("size", "50").param("sort", "status,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(purchaseOrderService).find(isNull(), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(1, 50, Sort.by(Sort.Direction.ASC, "status")));
    }

    @Test
    void findAllRejectsAnUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/purchase-orders").param("sort", "supplier"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot sort by 'supplier'")));

        verifyNoInteractions(purchaseOrderService);
    }

    @Test
    void receiveReturnsReceivedOrder() throws Exception {
        when(purchaseOrderService.receive(1L, (Long) null)).thenReturn(order(PurchaseOrderStatus.RECEIVED));

        mockMvc.perform(post("/api/purchase-orders/1/receive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void receivePassesOnTheWarehouseTheGoodsLandedIn() throws Exception {
        when(purchaseOrderService.receive(1L, 2L)).thenReturn(order(PurchaseOrderStatus.RECEIVED));

        mockMvc.perform(post("/api/purchase-orders/1/receive").param("warehouseId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        verify(purchaseOrderService).receive(1L, 2L);
    }

    @Test
    void receiptBooksTheDeliveredLinesAndReportsWhatIsOutstanding() throws Exception {
        PurchaseOrder partiallyReceived = order(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        partiallyReceived.getItems().get(0).setReceivedQuantity(1);
        when(purchaseOrderService.receive(eq(1L), any(GoodsReceiptRequest.class))).thenReturn(partiallyReceived);

        mockMvc.perform(post("/api/purchase-orders/1/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"itemId":11,"quantity":1}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_RECEIVED"))
                .andExpect(jsonPath("$.items[0].receivedQuantity").value(1))
                .andExpect(jsonPath("$.items[0].outstandingQuantity").value(3));

        ArgumentCaptor<GoodsReceiptRequest> request = ArgumentCaptor.captor();
        verify(purchaseOrderService).receive(eq(1L), request.capture());
        assertThat(request.getValue().lines()).singleElement()
                .satisfies(line -> assertThat(line.itemId()).isEqualTo(11L));
    }

    @Test
    void receiptCarriesTheWarehouseAndTheLotThroughToTheService() throws Exception {
        when(purchaseOrderService.receive(eq(1L), any(GoodsReceiptRequest.class)))
                .thenReturn(order(PurchaseOrderStatus.RECEIVED));

        mockMvc.perform(post("/api/purchase-orders/1/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"warehouseId":2,"lines":[
                                  {"itemId":11,"quantity":3,"lotCode":"A-2291","expiryDate":"2026-12-31"},
                                  {"itemId":11,"quantity":1,"warehouseId":3}
                                ]}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<GoodsReceiptRequest> request = ArgumentCaptor.captor();
        verify(purchaseOrderService).receive(eq(1L), request.capture());
        assertThat(request.getValue().warehouseId()).isEqualTo(2L);
        assertThat(request.getValue().lines()).satisfiesExactly(
                lot -> {
                    assertThat(lot.lotCode()).isEqualTo("A-2291");
                    assertThat(lot.expiryDate()).isEqualTo(LocalDate.of(2026, 12, 31));
                    assertThat(lot.warehouseId()).isNull();
                },
                elsewhere -> assertThat(elsewhere.warehouseId()).isEqualTo(3L));
    }

    @Test
    void receiptRejectsADeliveryWithNoLines() throws Exception {
        mockMvc.perform(post("/api/purchase-orders/1/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(purchaseOrderService);
    }

    @Test
    void receiptRejectsANonPositiveQuantity() throws Exception {
        mockMvc.perform(post("/api/purchase-orders/1/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"itemId":11,"quantity":0}]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(purchaseOrderService);
    }

    @Test
    void cancelReturnsCancelledOrder() throws Exception {
        when(purchaseOrderService.cancel(1L)).thenReturn(order(PurchaseOrderStatus.CANCELLED));

        mockMvc.perform(post("/api/purchase-orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

}
