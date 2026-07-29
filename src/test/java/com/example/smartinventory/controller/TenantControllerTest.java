package com.example.smartinventory.controller;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.smartinventory.exception.DuplicateTenantSlugException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Tenant;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.TenantService;
import com.example.smartinventory.tenant.TenantContext;

@WebMvcTest(controllers = TenantController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createReturnsCreatedTenant() throws Exception {
        Tenant tenant = Tenant.builder().id(1L).slug("acme").name("Acme").build();
        when(tenantService.create(any(Tenant.class))).thenReturn(tenant);

        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"acme","name":"Acme"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("acme"));
    }

    @Test
    void createReturnsConflictWhenSlugTaken() throws Exception {
        when(tenantService.create(any(Tenant.class)))
                .thenThrow(new DuplicateTenantSlugException("Tenant slug already in use: acme"));

        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"acme","name":"Acme"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createRejectsInvalidSlug() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"Acme Inc","name":"Acme"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAllReturnsTenants() throws Exception {
        when(tenantService.findAll()).thenReturn(List.of(Tenant.builder().id(1L).slug("acme").build()));

        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("acme"));
    }

    @Test
    void findCurrentReturnsCallersTenant() throws Exception {
        TenantContext.setTenantId("acme");
        when(tenantService.findBySlug("acme")).thenReturn(Tenant.builder().id(1L).slug("acme").name("Acme").build());

        mockMvc.perform(get("/api/tenants/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void findByIdReturnsTenant() throws Exception {
        when(tenantService.findById(1L)).thenReturn(Tenant.builder().id(1L).slug("acme").name("Acme").build());

        mockMvc.perform(get("/api/tenants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void findByIdReturnsNotFoundWhenMissing() throws Exception {
        when(tenantService.findById(99L)).thenThrow(new ResourceNotFoundException("Tenant not found with id: 99"));

        mockMvc.perform(get("/api/tenants/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsUpdatedTenant() throws Exception {
        Tenant tenant = Tenant.builder().id(1L).slug("acme").name("Acme Corp").build();
        when(tenantService.update(eq(1L), any(Tenant.class))).thenReturn(tenant);

        mockMvc.perform(put("/api/tenants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"acme","name":"Acme Corp"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Corp"));
    }

}
