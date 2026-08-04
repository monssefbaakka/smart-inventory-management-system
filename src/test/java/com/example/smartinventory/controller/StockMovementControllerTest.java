package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        when(stockMovementService.record(1L, null, null, MovementType.IN, 5, "restock", null)).thenReturn(movement);

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
        when(stockMovementService.record(1L, 7L, null, MovementType.IN, 5, null, null)).thenReturn(movement);

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
    void recordPassesTheStatedUnitCostThroughAndReportsWhatItWasValuedAt() throws Exception {
        Product product = Product.builder().id(1L).build();
        StockMovement movement = StockMovement.builder().id(3L).product(product).type(MovementType.IN).quantity(5)
                .unitCost(new BigDecimal("4.5000")).totalCost(new BigDecimal("22.5000")).build();
        when(stockMovementService.record(1L, null, null, MovementType.IN, 5, null, new BigDecimal("4.50")))
                .thenReturn(movement);

        mockMvc.perform(post("/api/products/1/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"IN","quantity":5,"unitCost":4.50}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitCost").value(4.5000))
                .andExpect(jsonPath("$.totalCost").value(22.5000));
    }

    @Test
    void recordRejectsANegativeUnitCost() throws Exception {
        mockMvc.perform(post("/api/products/1/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"IN","quantity":5,"unitCost":-1.00}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findByProductReturnsAPageOfHistory() throws Exception {
        Product product = Product.builder().id(1L).sku("SKU-1").name("Widget").build();
        StockMovement movement = StockMovement.builder().id(1L).product(product).type(MovementType.OUT).quantity(2)
                .build();
        when(stockMovementService.findByProduct(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movement), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/products/1/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void findByProductDefaultsToTheMostRecentFirst() throws Exception {
        when(stockMovementService.findByProduct(eq(1L), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/products/1/movements"))
                .andExpect(status().isOk());

        assertThat(capturedPageable())
                .isEqualTo(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void findByProductHonoursThePagingAndSortingAsked() throws Exception {
        when(stockMovementService.findByProduct(eq(1L), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/products/1/movements")
                        .param("page", "2").param("size", "5").param("sort", "quantity,asc"))
                .andExpect(status().isOk());

        assertThat(capturedPageable()).isEqualTo(PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "quantity")));
    }

    @Test
    void findByProductRejectsAnUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/products/1/movements").param("sort", "note"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot sort by 'note'")));

        verifyNoInteractions(stockMovementService);
    }

    @Test
    void findByProductRejectsAPageSizeBeyondTheCap() throws Exception {
        mockMvc.perform(get("/api/products/1/movements").param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stockMovementService);
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(stockMovementService).findByProduct(eq(1L), pageable.capture());
        return pageable.getValue();
    }

}
