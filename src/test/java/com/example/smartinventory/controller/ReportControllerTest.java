package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.smartinventory.dto.CostOfGoodsSoldResponse;
import com.example.smartinventory.dto.InventoryValuationLine;
import com.example.smartinventory.dto.InventoryValuationResponse;
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

    @Test
    void exportProductsXlsxReturnsDownloadableWorkbook() throws Exception {
        byte[] workbook = {1, 2, 3, 4};
        when(reportService.exportProductsXlsx()).thenReturn(workbook);

        mockMvc.perform(get("/api/reports/products.xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"products.xlsx\""))
                .andExpect(content().bytes(workbook));
    }

    @Test
    void exportProductsPdfReturnsDownloadablePdf() throws Exception {
        byte[] pdf = "%PDF-1.4 body %%EOF".getBytes();
        when(reportService.exportProductsPdf()).thenReturn(pdf);

        mockMvc.perform(get("/api/reports/products.pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"products.pdf\""))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void exportStockMovementsCsvReturnsDownloadableCsv() throws Exception {
        String csv = "id,productId,productSku,type,quantity,note,createdAt\r\n"
                + "9,3,SKU-3,IN,5,restock,2026-01-02T03:04:05Z\r\n";
        when(reportService.exportStockMovementsCsv()).thenReturn(csv);

        mockMvc.perform(get("/api/reports/export/movements"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"movements.csv\""))
                .andExpect(content().string(csv));
    }

    @Test
    void valuationReturnsTheLinesAndTheTotal() throws Exception {
        InventoryValuationLine line = new InventoryValuationLine(1L, "SKU-1", "Hammer", 3,
                new BigDecimal("10.0000"), new BigDecimal("30.0000"));
        when(reportService.valuation())
                .thenReturn(new InventoryValuationResponse(List.of(line), new BigDecimal("30.0000")));

        mockMvc.perform(get("/api/reports/valuation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$.products[0].averageCost").value(10.0000))
                .andExpect(jsonPath("$.products[0].value").value(30.0000))
                .andExpect(jsonPath("$.total").value(30.0000));
    }

    @Test
    void costOfGoodsSoldPassesTheWindowAndProductThrough() throws Exception {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        when(reportService.costOfGoodsSold(from, to, 5L))
                .thenReturn(new CostOfGoodsSoldResponse(from, to, 5L, 4L, new BigDecimal("18.0000")));

        mockMvc.perform(get("/api/reports/cogs")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z")
                        .param("productId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(5))
                .andExpect(jsonPath("$.quantity").value(4))
                .andExpect(jsonPath("$.totalCost").value(18.0000));
    }

    @Test
    void costOfGoodsSoldWithoutAWindowCoversTheWholeRecord() throws Exception {
        when(reportService.costOfGoodsSold(eq(Instant.EPOCH), any(Instant.class), isNull()))
                .thenReturn(new CostOfGoodsSoldResponse(Instant.EPOCH, Instant.parse("2026-08-04T00:00:00Z"), null,
                        120L, new BigDecimal("600.0000")));

        mockMvc.perform(get("/api/reports/cogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(120))
                .andExpect(jsonPath("$.productId").doesNotExist());
    }

}
