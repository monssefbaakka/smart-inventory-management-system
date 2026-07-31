package com.example.smartinventory.controller;

import java.time.Instant;
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

import com.example.smartinventory.dto.StockCountLineRequest;
import com.example.smartinventory.dto.StockCountLineResponse;
import com.example.smartinventory.dto.StockCountRequest;
import com.example.smartinventory.dto.StockCountResponse;
import com.example.smartinventory.exception.InvalidStockCountStateException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.StockCountStatus;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.StockCountService;

@WebMvcTest(controllers = StockCountController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class StockCountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockCountService stockCountService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void openReturnsCreatedCount() throws Exception {
        when(stockCountService.open(any(StockCountRequest.class))).thenReturn(response(StockCountStatus.DRAFT));

        mockMvc.perform(post("/api/stock-counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"warehouseId":7,"note":"quarterly"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.warehouseCode").value("WH-1"));
    }

    @Test
    void openReturnsBadRequestWithoutAWarehouse() throws Exception {
        mockMvc.perform(post("/api/stock-counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"quarterly"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addLineReturnsCountWithTheLine() throws Exception {
        when(stockCountService.addLine(eq(3L), any(StockCountLineRequest.class)))
                .thenReturn(response(StockCountStatus.DRAFT));

        mockMvc.perform(post("/api/stock-counts/3/lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"countedQuantity":38}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].variance").value(-4))
                .andExpect(jsonPath("$.totalVariance").value(-4));
    }

    @Test
    void addLineReturnsBadRequestForANegativeCount() throws Exception {
        mockMvc.perform(post("/api/stock-counts/3/lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"countedQuantity":-1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addLineReturnsConflictOnceTheCountIsClosed() throws Exception {
        when(stockCountService.addLine(eq(3L), any(StockCountLineRequest.class)))
                .thenThrow(new InvalidStockCountStateException("Stock count 3 cannot be counted from status "
                        + "COMPLETED; expected DRAFT"));

        mockMvc.perform(post("/api/stock-counts/3/lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"countedQuantity":38}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void completeReturnsCompletedCount() throws Exception {
        when(stockCountService.complete(3L)).thenReturn(response(StockCountStatus.COMPLETED));

        mockMvc.perform(post("/api/stock-counts/3/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void completeReturnsConflictWhenNothingWasCounted() throws Exception {
        when(stockCountService.complete(3L)).thenThrow(new InvalidStockCountStateException(
                "Stock count 3 cannot be completed because nothing was counted"));

        mockMvc.perform(post("/api/stock-counts/3/complete"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelReturnsCancelledCount() throws Exception {
        when(stockCountService.cancel(3L)).thenReturn(response(StockCountStatus.CANCELLED));

        mockMvc.perform(post("/api/stock-counts/3/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void findByIdReturnsCount() throws Exception {
        when(stockCountService.findById(3L)).thenReturn(response(StockCountStatus.DRAFT));

        mockMvc.perform(get("/api/stock-counts/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].sku").value("SKU-1"));
    }

    @Test
    void findByIdReturnsNotFoundWhenMissing() throws Exception {
        when(stockCountService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Stock count not found with id: 99"));

        mockMvc.perform(get("/api/stock-counts/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findReturnsAPageOfEveryCountWithoutFilters() throws Exception {
        when(stockCountService.find(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response(StockCountStatus.DRAFT)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/stock-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void findPassesWarehouseAndStatusFiltersThrough() throws Exception {
        when(stockCountService.find(eq(7L), eq(StockCountStatus.COMPLETED), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/stock-counts").param("warehouseId", "7").param("status", "COMPLETED"))
                .andExpect(status().isOk());

        verify(stockCountService).find(eq(7L), eq(StockCountStatus.COMPLETED), any(Pageable.class));
    }

    @Test
    void findDefaultsToTheMostRecentFirst() throws Exception {
        when(stockCountService.find(isNull(), isNull(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/stock-counts"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(stockCountService).find(isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue())
                .isEqualTo(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void findHonoursThePagingAndSortingAsked() throws Exception {
        when(stockCountService.find(isNull(), isNull(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/stock-counts").param("page", "3").param("size", "10").param("sort", "status,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(stockCountService).find(isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(3, 10, Sort.by(Sort.Direction.ASC, "status")));
    }

    @Test
    void findRejectsAnUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/stock-counts").param("sort", "warehouse"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot sort by 'warehouse'")));

        verifyNoInteractions(stockCountService);
    }

    private static StockCountResponse response(StockCountStatus status) {
        StockCountLineResponse line = new StockCountLineResponse(9L, 1L, "SKU-1", "Widget", 38, 42, -4);
        return new StockCountResponse(3L, 7L, "WH-1", "Main Depot", status, "quarterly", List.of(line), -4,
                Instant.parse("2026-07-28T10:15:30Z"),
                status == StockCountStatus.COMPLETED ? Instant.parse("2026-07-28T11:00:00Z") : null);
    }

}
