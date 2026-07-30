package com.example.smartinventory.controller;

import java.util.List;

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

import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.StockMovementService;

@WebMvcTest(controllers = StockMovementController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class StockMovementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockMovementService stockMovementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void recordReturnsCreatedMovement() throws Exception {
        Product product = Product.builder().id(1L).sku("SKU-1").name("Widget").build();
        StockMovement movement = StockMovement.builder().id(1L).product(product).type(MovementType.IN).quantity(5)
                .build();
        when(stockMovementService.record(1L, null, MovementType.IN, 5, "restock")).thenReturn(movement);

        mockMvc.perform(post("/api/products/1/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"IN","quantity":5,"note":"restock"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("IN"))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-1"))
                .andExpect(jsonPath("$.productName").value("Widget"))
                .andExpect(jsonPath("$.warehouseId").doesNotExist())
                .andExpect(jsonPath("$.product").doesNotExist());
    }

    @Test
    void recordPassesWarehouseThrough() throws Exception {
        Product product = Product.builder().id(1L).build();
        Warehouse warehouse = Warehouse.builder().id(7L).code("WH-1").name("Main Depot").build();
        StockMovement movement = StockMovement.builder().id(2L).product(product).warehouse(warehouse)
                .type(MovementType.IN).quantity(5).build();
        when(stockMovementService.record(1L, 7L, MovementType.IN, 5, null)).thenReturn(movement);

        mockMvc.perform(post("/api/products/1/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"IN","quantity":5,"warehouseId":7}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warehouseId").value(7))
                .andExpect(jsonPath("$.warehouseCode").value("WH-1"));
    }

    @Test
    void findByProductReturnsHistory() throws Exception {
        Product product = Product.builder().id(1L).sku("SKU-1").name("Widget").build();
        StockMovement movement = StockMovement.builder().id(1L).product(product).type(MovementType.OUT).quantity(2)
                .build();
        when(stockMovementService.findByProduct(1L)).thenReturn(List.of(movement));

        mockMvc.perform(get("/api/products/1/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].sku").value("SKU-1"));
    }

}
