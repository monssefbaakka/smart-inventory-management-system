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

import com.example.smartinventory.dto.StockTransferResponse;
import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.exception.InvalidStockTransferException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.StockTransferService;

@WebMvcTest(controllers = StockTransferController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class StockTransferControllerTest {

    private static final String REQUEST_BODY = """
            {"productId":1,"sourceWarehouseId":1,"destinationWarehouseId":2,"quantity":6,"note":"rebalancing"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockTransferService stockTransferService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void transferReturnsCreatedTransfer() throws Exception {
        when(stockTransferService.transfer(1L, 1L, 2L, 6, "rebalancing")).thenReturn(response());

        mockMvc.perform(post("/api/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.sourceWarehouseCode").value("WH-NORTH"))
                .andExpect(jsonPath("$.destinationWarehouseCode").value("WH-SOUTH"))
                .andExpect(jsonPath("$.quantity").value(6));
    }

    @Test
    void transferReturnsBadRequestWhenQuantityIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"sourceWarehouseId":1,"destinationWarehouseId":2,"quantity":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferReturnsBadRequestWhenBothSidesNameTheSameWarehouse() throws Exception {
        when(stockTransferService.transfer(1L, 1L, 2L, 6, "rebalancing"))
                .thenThrow(new InvalidStockTransferException("Source and destination warehouse must differ"));

        mockMvc.perform(post("/api/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferReturnsConflictWhenSourceHoldsTooLittle() throws Exception {
        when(stockTransferService.transfer(1L, 1L, 2L, 6, "rebalancing"))
                .thenThrow(new InsufficientStockException("only 2 in stock"));

        mockMvc.perform(post("/api/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void findByIdReturnsTransfer() throws Exception {
        when(stockTransferService.findById(5L)).thenReturn(response());

        mockMvc.perform(get("/api/stock-transfers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1"));
    }

    @Test
    void findByIdReturnsNotFoundWhenMissing() throws Exception {
        when(stockTransferService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Stock transfer not found with id: 99"));

        mockMvc.perform(get("/api/stock-transfers/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findReturnsAPageOfTheWholeHistoryWithoutFilters() throws Exception {
        when(stockTransferService.find(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/stock-transfers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void findPassesProductFilterThrough() throws Exception {
        when(stockTransferService.find(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/stock-transfers").param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(1));

        verify(stockTransferService).find(eq(1L), isNull(), any(Pageable.class));
    }

    @Test
    void findPassesWarehouseFilterThrough() throws Exception {
        when(stockTransferService.find(isNull(), eq(2L), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/stock-transfers").param("warehouseId", "2"))
                .andExpect(status().isOk());

        verify(stockTransferService).find(isNull(), eq(2L), any(Pageable.class));
    }

    @Test
    void findDefaultsToTheMostRecentFirst() throws Exception {
        when(stockTransferService.find(isNull(), isNull(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/stock-transfers"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(stockTransferService).find(isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue())
                .isEqualTo(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void findHonoursThePagingAndSortingAsked() throws Exception {
        when(stockTransferService.find(isNull(), isNull(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/stock-transfers").param("page", "1").param("size", "3").param("sort", "quantity"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(stockTransferService).find(isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(1, 3, Sort.by(Sort.Direction.ASC, "quantity")));
    }

    @Test
    void findRejectsAnUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/stock-transfers").param("sort", "note"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot sort by 'note'")));

        verifyNoInteractions(stockTransferService);
    }

    private static StockTransferResponse response() {
        return new StockTransferResponse(5L, 1L, "SKU-1", "Widget", 1L, "WH-NORTH", 2L, "WH-SOUTH", 6,
                "rebalancing", Instant.parse("2026-07-28T10:15:30Z"));
    }

}
