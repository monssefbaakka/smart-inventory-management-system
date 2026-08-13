package com.example.smartinventory.controller;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.StockLevelService;

@WebMvcTest(controllers = ProductStockController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductStockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockLevelService stockLevelService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void findByProductReturnsBreakdownAcrossWarehouses() throws Exception {
        StockLevelResponse north = new StockLevelResponse(1L, "SKU-1", "Widget", 7L, "WH-N", "North", 12, null);
        StockLevelResponse south = new StockLevelResponse(1L, "SKU-1", "Widget", 8L, "WH-S", "South", 3, 5);
        when(stockLevelService.findByProduct(1L)).thenReturn(List.of(north, south));

        mockMvc.perform(get("/api/products/1/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouseCode").value("WH-N"))
                .andExpect(jsonPath("$[1].quantity").value(3));
    }

    @Test
    void findByProductReportsTheReorderPointASiteHoldsOfItsOwn() throws Exception {
        StockLevelResponse level = new StockLevelResponse(1L, "SKU-1", "Widget", 8L, "WH-S", "South", 3, 5);
        when(stockLevelService.findByProduct(1L)).thenReturn(List.of(level));

        mockMvc.perform(get("/api/products/1/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reorderThreshold").value(5));
    }

    @Test
    void findByProductReturnsNotFoundWhenProductMissing() throws Exception {
        when(stockLevelService.findByProduct(99L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99/stock"))
                .andExpect(status().isNotFound());
    }

    @Test
    void setReorderThresholdRecordsItAgainstTheWarehouse() throws Exception {
        StockLevelResponse level = new StockLevelResponse(1L, "SKU-1", "Widget", 8L, "WH-S", "South", 3, 5);
        when(stockLevelService.setReorderThreshold(1L, 8L, 5)).thenReturn(level);

        mockMvc.perform(put("/api/products/1/stock/8/reorder-threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reorderThreshold":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseCode").value("WH-S"))
                .andExpect(jsonPath("$.reorderThreshold").value(5));
    }

    @Test
    void setReorderThresholdClearsItWhenGivenNull() throws Exception {
        StockLevelResponse level = new StockLevelResponse(1L, "SKU-1", "Widget", 8L, "WH-S", "South", 3, null);
        when(stockLevelService.setReorderThreshold(1L, 8L, null)).thenReturn(level);

        mockMvc.perform(put("/api/products/1/stock/8/reorder-threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reorderThreshold":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reorderThreshold").doesNotExist());
    }

    @Test
    void setReorderThresholdRejectsANegativeThreshold() throws Exception {
        mockMvc.perform(put("/api/products/1/stock/8/reorder-threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reorderThreshold":-1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setReorderThresholdReturnsNotFoundWhenWarehouseMissing() throws Exception {
        when(stockLevelService.setReorderThreshold(1L, 99L, 5))
                .thenThrow(new ResourceNotFoundException("Warehouse not found with id: 99"));

        mockMvc.perform(put("/api/products/1/stock/99/reorder-threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reorderThreshold":5}
                                """))
                .andExpect(status().isNotFound());
    }

}
