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
    void exportProductsCsvReturnsDownloadableCsv() throws Exception {
        String csv = "id,sku,name,category,quantity,price,stock_value\r\n1,SKU-1,Hammer,Tools,3,10.00,30.00\r\n";
        when(reportService.exportProductsCsv()).thenReturn(csv);

        mockMvc.perform(get("/api/reports/products.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"products.csv\""))
                .andExpect(content().string(csv));
    }

}
