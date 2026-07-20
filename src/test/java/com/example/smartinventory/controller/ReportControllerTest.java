package com.example.smartinventory.controller;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.ReportService;

@WebMvcTest(controllers = ReportController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void totalStockValueReturnsOk() throws Exception {
        when(reportService.totalStockValue()).thenReturn(new BigDecimal("123.45"));

        mockMvc.perform(get("/api/reports/stock-value"))
                .andExpect(status().isOk())
                .andExpect(content().string("123.45"));
    }

    @Test
    void exportProductsCsvReturnsAttachment() throws Exception {
        when(reportService.exportProductsToCsv()).thenReturn("id,sku,name,category,quantity,price,stockValue\n");

        mockMvc.perform(get("/api/reports/export/products"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"products.csv\""))
                .andExpect(content().string("id,sku,name,category,quantity,price,stockValue\n"));
    }

}
