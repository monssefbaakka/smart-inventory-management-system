package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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

import com.example.smartinventory.dto.PurchaseOrderRequest;
import com.example.smartinventory.dto.PurchaseOrderResponse;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.model.PurchaseOrderItem;
import com.example.smartinventory.model.PurchaseOrderStatus;
import com.example.smartinventory.model.Supplier;
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
                .andExpect(jsonPath("$.total").value(10.00))
                .andExpect(jsonPath("$.items[0].productId").value(3))
                .andExpect(jsonPath("$.items[0].sku").value("SKU-3"))
                .andExpect(jsonPath("$.items[0].productName").value("Widget"))
                .andExpect(jsonPath("$.items[0].lineTotal").value(10.00))
                .andExpect(jsonPath("$.supplier").doesNotExist())
                .andExpect(jsonPath("$.items[0].product").doesNotExist());
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
        when(purchaseOrderService.receive(1L)).thenReturn(order(PurchaseOrderStatus.RECEIVED));

        mockMvc.perform(post("/api/purchase-orders/1/receive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void cancelReturnsCancelledOrder() throws Exception {
        when(purchaseOrderService.cancel(1L)).thenReturn(order(PurchaseOrderStatus.CANCELLED));

        mockMvc.perform(post("/api/purchase-orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

}
