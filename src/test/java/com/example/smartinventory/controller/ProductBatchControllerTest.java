package com.example.smartinventory.controller;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.example.smartinventory.dto.ProductBatchRequest;
import com.example.smartinventory.exception.InvalidBatchStateException;
import com.example.smartinventory.exception.InvalidQueryParameterException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.ProductBatch;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.ProductBatchService;

@WebMvcTest(controllers = ProductBatchController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductBatchControllerTest {

    private static final Product PRODUCT = Product.builder().id(1L).sku("SKU-1").name("Yoghurt").build();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductBatchService productBatchService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void createReturnsTheDeclaredBatch() throws Exception {
        when(productBatchService.create(eq(1L), any(ProductBatchRequest.class)))
                .thenReturn(batch(4L, "A-2291", LocalDate.of(2026, 12, 31), 0));

        mockMvc.perform(post("/api/products/1/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lotCode":"A-2291","expiryDate":"2026-12-31"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.lotCode").value("A-2291"))
                .andExpect(jsonPath("$.expiryDate").value("2026-12-31"))
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productSku").value("SKU-1"))
                .andExpect(jsonPath("$.product").doesNotExist());
    }

    @Test
    void createRejectsABlankLotCode() throws Exception {
        mockMvc.perform(post("/api/products/1/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lotCode":"  "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReportsALotCodeTheProductAlreadyCarries() throws Exception {
        when(productBatchService.create(eq(1L), any(ProductBatchRequest.class)))
                .thenThrow(new InvalidBatchStateException("Product 1 already has a batch with lot code A-2291"));

        mockMvc.perform(post("/api/products/1/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lotCode":"A-2291"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void findByProductReturnsTheLotsWithTheirWarehouse() throws Exception {
        ProductBatch batch = batch(4L, "A-2291", LocalDate.of(2026, 12, 31), 40);
        batch.setWarehouse(Warehouse.builder().id(7L).code("WH-1").build());
        when(productBatchService.findByProduct(1L)).thenReturn(List.of(batch));

        mockMvc.perform(get("/api/products/1/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lotCode").value("A-2291"))
                .andExpect(jsonPath("$[0].quantity").value(40))
                .andExpect(jsonPath("$[0].warehouseId").value(7))
                .andExpect(jsonPath("$[0].warehouseCode").value("WH-1"));
    }

    @Test
    void findByIdReturnsNotFoundWhenMissing() throws Exception {
        when(productBatchService.findById(99L)).thenThrow(new ResourceNotFoundException("Batch not found with id: 99"));

        mockMvc.perform(get("/api/batches/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findExpiringDefaultsToThirtyDays() throws Exception {
        when(productBatchService.findExpiringWithin(30))
                .thenReturn(List.of(batch(4L, "A-2291", LocalDate.now().plusDays(3), 40)));

        mockMvc.perform(get("/api/batches/expiring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lotCode").value("A-2291"));

        verify(productBatchService).findExpiringWithin(30);
    }

    @Test
    void findExpiringPassesTheWindowThrough() throws Exception {
        when(productBatchService.findExpiringWithin(7)).thenReturn(List.of());

        mockMvc.perform(get("/api/batches/expiring?days=7"))
                .andExpect(status().isOk());

        verify(productBatchService).findExpiringWithin(7);
    }

    @Test
    void findExpiringRejectsANegativeWindow() throws Exception {
        when(productBatchService.findExpiringWithin(-1))
                .thenThrow(new InvalidQueryParameterException("days must not be negative, but was -1"));

        mockMvc.perform(get("/api/batches/expiring?days=-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findExpiredMarksTheLotsAsExpired() throws Exception {
        when(productBatchService.findExpired())
                .thenReturn(List.of(batch(4L, "A-2291", LocalDate.now().minusDays(1), 12)));

        mockMvc.perform(get("/api/batches/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].expired").value(true));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/batches/4"))
                .andExpect(status().isNoContent());

        verify(productBatchService).delete(4L);
    }

    @Test
    void deleteReportsALotThatStillHoldsStock() throws Exception {
        doThrow(new InvalidBatchStateException("Batch 4 still holds 12 units"))
                .when(productBatchService).delete(4L);

        mockMvc.perform(delete("/api/batches/4"))
                .andExpect(status().isConflict());
    }

    private ProductBatch batch(Long id, String lotCode, LocalDate expiryDate, int quantity) {
        return ProductBatch.builder()
                .id(id)
                .product(PRODUCT)
                .lotCode(lotCode)
                .expiryDate(expiryDate)
                .quantity(quantity)
                .build();
    }

}
