package com.example.smartinventory.controller;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.smartinventory.dto.PurchaseOrderRequest;
import com.example.smartinventory.model.PurchaseOrder;
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
        return PurchaseOrder.builder().id(1L).supplier(Supplier.builder().id(7L).build()).status(status).build();
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
                .andExpect(jsonPath("$.status").value("DRAFT"));
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
    void findAllReturnsOrders() throws Exception {
        when(purchaseOrderService.findAll()).thenReturn(List.of(order(PurchaseOrderStatus.DRAFT)));

        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void findAllFiltersBySupplierWhenProvided() throws Exception {
        when(purchaseOrderService.findBySupplier(7L)).thenReturn(List.of(order(PurchaseOrderStatus.PLACED)));

        mockMvc.perform(get("/api/purchase-orders").param("supplierId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PLACED"));
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
