package com.example.smartinventory.controller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.smartinventory.model.AuditAction;
import com.example.smartinventory.model.AuditLog;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.AuditService;

@WebMvcTest(controllers = AuditLogController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void findAllReturnsAPageOfAuditEntries() throws Exception {
        when(auditService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("Product"))
                .andExpect(jsonPath("$.content[0].entityId").value(42))
                .andExpect(jsonPath("$.content[0].action").value("CREATE"))
                .andExpect(jsonPath("$.content[0].username").value("alice"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findAllDoesNotLeakTheTenantDiscriminator() throws Exception {
        when(auditService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tenantId").doesNotExist());
    }

    @Test
    void findAllDefaultsToTheMostRecentFirst() throws Exception {
        when(auditService.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk());

        assertThat(capturedPageable())
                .isEqualTo(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void findAllHonoursThePagingAndSortingAsked() throws Exception {
        when(auditService.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/audit-logs").param("page", "4").param("size", "25").param("sort", "username,asc"))
                .andExpect(status().isOk());

        assertThat(capturedPageable()).isEqualTo(PageRequest.of(4, 25, Sort.by(Sort.Direction.ASC, "username")));
    }

    @Test
    void findAllRejectsAnUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/audit-logs").param("sort", "tenantId"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot sort by 'tenantId'")));

        verifyNoInteractions(auditService);
    }

    @Test
    void findAllRejectsAPageSizeBeyondTheCap() throws Exception {
        mockMvc.perform(get("/api/audit-logs").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("size must be between 1 and 100")));

        verifyNoInteractions(auditService);
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(auditService).findAll(pageable.capture());
        return pageable.getValue();
    }

    private static AuditLog entry() {
        return AuditLog.builder().id(1L).tenantId("acme").entityType("Product").entityId(42L)
                .action(AuditAction.CREATE).username("alice").build();
    }

}
