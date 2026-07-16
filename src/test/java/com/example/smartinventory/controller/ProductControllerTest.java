package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.ProductService;

@WebMvcTest(controllers = ProductController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void createReturnsCreatedProduct() throws Exception {
        Product product = Product.builder().id(1L).name("Widget").sku("SKU-1").price(BigDecimal.TEN).quantity(3)
                .build();
        when(productService.create(any(Product.class))).thenReturn(product);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Widget","sku":"SKU-1","price":10,"quantity":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findByIdReturnsProduct() throws Exception {
        Product product = Product.builder().id(1L).name("Widget").build();
        when(productService.findById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Widget"));
    }

    @Test
    void findByIdReturnsNotFoundWhenMissing() throws Exception {
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAllReturnsProducts() throws Exception {
        Product product = Product.builder().id(1L).name("Widget").build();
        when(productService.findAll()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void updateReturnsUpdatedProduct() throws Exception {
        Product product = Product.builder().id(1L).name("Updated").sku("SKU-1").price(BigDecimal.TEN).quantity(3)
                .build();
        when(productService.update(eq(1L), any(Product.class))).thenReturn(product);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated","sku":"SKU-1","price":10,"quantity":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).delete(1L);
    }

}
