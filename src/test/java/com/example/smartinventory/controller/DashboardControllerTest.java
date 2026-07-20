package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
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

import com.example.smartinventory.dto.DashboardSummaryResponse;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.DashboardService;

@WebMvcTest(controllers = DashboardController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void summaryReturnsOk() throws Exception {
        DashboardSummaryResponse summary = DashboardSummaryResponse.builder()
                .totalProducts(7)
                .totalCategories(3)
                .totalSuppliers(2)
                .lowStockCount(1)
                .totalStockValue(new BigDecimal("199.99"))
                .build();
        when(dashboardService.summary()).thenReturn(summary);

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(7))
                .andExpect(jsonPath("$.totalCategories").value(3))
                .andExpect(jsonPath("$.totalSuppliers").value(2))
                .andExpect(jsonPath("$.lowStockCount").value(1))
                .andExpect(jsonPath("$.totalStockValue").value(199.99));
    }

    @Test
    void recentMovementsReturnsOk() throws Exception {
        Product product = Product.builder().id(1L).build();
        StockMovement movement = StockMovement.builder().id(1L).product(product).type(MovementType.IN).quantity(5)
                .build();
        when(dashboardService.recentMovements(5)).thenReturn(List.of(movement));

        mockMvc.perform(get("/api/dashboard/recent-movements").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void recentMovementsDefaultsLimitToTen() throws Exception {
        when(dashboardService.recentMovements(eq(10))).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/recent-movements"))
                .andExpect(status().isOk());
    }

    @Test
    void lowStockReturnsOk() throws Exception {
        Product product = Product.builder().id(1L).quantity(1).reorderThreshold(5).build();
        when(dashboardService.lowStockProducts()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/dashboard/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

}
