package com.example.smartinventory.controller;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
        StockLevelResponse north = new StockLevelResponse(1L, "SKU-1", "Widget", 7L, "WH-N", "North", 12);
        StockLevelResponse south = new StockLevelResponse(1L, "SKU-1", "Widget", 8L, "WH-S", "South", 3);
        when(stockLevelService.findByProduct(1L)).thenReturn(List.of(north, south));

        mockMvc.perform(get("/api/products/1/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouseCode").value("WH-N"))
                .andExpect(jsonPath("$[1].quantity").value(3));
    }

    @Test
    void findByProductReturnsNotFoundWhenProductMissing() throws Exception {
        when(stockLevelService.findByProduct(99L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99/stock"))
                .andExpect(status().isNotFound());
    }

}
