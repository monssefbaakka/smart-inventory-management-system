package com.example.smartinventory.controller;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.example.smartinventory.dto.StockLevelResponse;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.StockLevelService;
import com.example.smartinventory.service.WarehouseService;

@WebMvcTest(controllers = WarehouseController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarehouseService warehouseService;

    @MockitoBean
    private StockLevelService stockLevelService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void createReturnsCreatedWarehouse() throws Exception {
        Warehouse warehouse = Warehouse.builder().id(1L).code("WH-1").name("Main Depot").build();
        when(warehouseService.create(any(Warehouse.class))).thenReturn(warehouse);

        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"WH-1","name":"Main Depot"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("WH-1"));
    }

    @Test
    void findByIdReturnsWarehouse() throws Exception {
        when(warehouseService.findById(1L)).thenReturn(Warehouse.builder().id(1L).name("Main Depot").build());

        mockMvc.perform(get("/api/warehouses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Main Depot"));
    }

    @Test
    void findByIdReturnsNotFoundWhenMissing() throws Exception {
        when(warehouseService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Warehouse not found with id: 99"));

        mockMvc.perform(get("/api/warehouses/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAllReturnsWarehouses() throws Exception {
        when(warehouseService.findAll()).thenReturn(List.of(Warehouse.builder().id(1L).build()));

        mockMvc.perform(get("/api/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void findStockReturnsLevelsHeldInWarehouse() throws Exception {
        StockLevelResponse level = new StockLevelResponse(1L, "SKU-1", "Widget", 7L, "WH-1", "Main Depot", 12, null);
        when(stockLevelService.findByWarehouse(7L)).thenReturn(List.of(level));

        mockMvc.perform(get("/api/warehouses/7/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$[0].quantity").value(12));
    }

    @Test
    void updateReturnsUpdatedWarehouse() throws Exception {
        Warehouse warehouse = Warehouse.builder().id(1L).code("WH-1").name("Renamed").build();
        when(warehouseService.update(eq(1L), any(Warehouse.class))).thenReturn(warehouse);

        mockMvc.perform(put("/api/warehouses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"WH-1","name":"Renamed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/warehouses/1"))
                .andExpect(status().isNoContent());

        verify(warehouseService).delete(1L);
    }

}
