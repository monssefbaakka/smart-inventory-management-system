package com.example.smartinventory.controller;

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
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.SupplierService;

@WebMvcTest(controllers = SupplierController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierService supplierService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void createReturnsCreatedSupplier() throws Exception {
        Supplier supplier = Supplier.builder().id(1L).name("Acme").email("acme@example.com").build();
        when(supplierService.create(any(Supplier.class))).thenReturn(supplier);

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme","email":"acme@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findByIdReturnsSupplier() throws Exception {
        Supplier supplier = Supplier.builder().id(1L).name("Acme").build();
        when(supplierService.findById(1L)).thenReturn(supplier);

        mockMvc.perform(get("/api/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void findByIdReturnsNotFoundWhenMissing() throws Exception {
        when(supplierService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Supplier not found with id: 99"));

        mockMvc.perform(get("/api/suppliers/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAllReturnsSuppliers() throws Exception {
        Supplier supplier = Supplier.builder().id(1L).name("Acme").build();
        when(supplierService.findAll()).thenReturn(List.of(supplier));

        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void updateReturnsUpdatedSupplier() throws Exception {
        Supplier supplier = Supplier.builder().id(1L).name("Updated").email("acme@example.com").build();
        when(supplierService.update(eq(1L), any(Supplier.class))).thenReturn(supplier);

        mockMvc.perform(put("/api/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated","email":"acme@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/suppliers/1"))
                .andExpect(status().isNoContent());

        verify(supplierService).delete(1L);
    }

}
