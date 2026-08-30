package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.smartinventory.dto.SupplierReliabilityResponse;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.model.Warehouse;
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
    void theReliabilityTableIsReturnedInTheOrderTheServiceRanksIt() throws Exception {
        when(supplierService.reliability((LocalDate) null)).thenReturn(List.of(
                new SupplierReliabilityResponse(7L, "Acme Supplies", 2, 1, 1,
                        new BigDecimal("0.50"), new BigDecimal("4.0"), 4L,
                        new BigDecimal("500.00"), new BigDecimal("400.00")),
                new SupplierReliabilityResponse(9L, "Cove Trading", 0, 0, 0, null, null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO)));

        mockMvc.perform(get("/api/suppliers/reliability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].supplierName").value("Acme Supplies"))
                .andExpect(jsonPath("$[0].onTimeRate").value(0.50))
                .andExpect(jsonPath("$[1].supplierName").value("Cove Trading"))
                .andExpect(jsonPath("$[0].judgedSpend").value(500.00))
                .andExpect(jsonPath("$[0].lateSpend").value(400.00))
                .andExpect(jsonPath("$[1].ordersJudged").value(0))
                .andExpect(jsonPath("$[1].onTimeRate").value(nullValue()))
                .andExpect(jsonPath("$[1].judgedSpend").value(0));
    }

    @Test
    void reliabilityReportsHowWellTheSupplierKeepsTheirDates() throws Exception {
        when(supplierService.reliability(7L, null)).thenReturn(new SupplierReliabilityResponse(
                7L, "Acme Supplies", 4, 2, 2, new BigDecimal("0.50"), new BigDecimal("7.0"), 11L,
                new BigDecimal("1390.50"), new BigDecimal("1040.50")));

        mockMvc.perform(get("/api/suppliers/7/reliability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierId").value(7))
                .andExpect(jsonPath("$.supplierName").value("Acme Supplies"))
                .andExpect(jsonPath("$.ordersJudged").value(4))
                .andExpect(jsonPath("$.onTime").value(2))
                .andExpect(jsonPath("$.late").value(2))
                .andExpect(jsonPath("$.onTimeRate").value(0.50))
                .andExpect(jsonPath("$.averageDaysLate").value(7.0))
                .andExpect(jsonPath("$.worstDaysLate").value(11))
                .andExpect(jsonPath("$.judgedSpend").value(1390.50))
                .andExpect(jsonPath("$.lateSpend").value(1040.50));
    }

    @Test
    void reliabilityReportsNothingJudgedAsNullFigures() throws Exception {
        when(supplierService.reliability(7L, null)).thenReturn(new SupplierReliabilityResponse(
                7L, "Acme Supplies", 0, 0, 0, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO));

        mockMvc.perform(get("/api/suppliers/7/reliability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersJudged").value(0))
                .andExpect(jsonPath("$.onTimeRate").value(nullValue()))
                .andExpect(jsonPath("$.averageDaysLate").value(nullValue()))
                .andExpect(jsonPath("$.worstDaysLate").value(nullValue()))
                .andExpect(jsonPath("$.judgedSpend").value(0))
                .andExpect(jsonPath("$.lateSpend").value(0));
    }

    @Test
    void reliabilityAnswersNotFoundForASupplierThatDoesNotExist() throws Exception {
        when(supplierService.reliability(9L, null)).thenThrow(new ResourceNotFoundException("Supplier not found"));

        mockMvc.perform(get("/api/suppliers/9/reliability"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reliabilityCarriesTheWindowItWasAskedForThroughToTheService() throws Exception {
        when(supplierService.reliability(7L, LocalDate.of(2026, 1, 1))).thenReturn(
                new SupplierReliabilityResponse(7L, "Acme Supplies", 2, 1, 1, new BigDecimal("0.50"),
                        new BigDecimal("6.0"), 6L, new BigDecimal("500.00"), new BigDecimal("400.00")));

        mockMvc.perform(get("/api/suppliers/7/reliability").param("since", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersJudged").value(2))
                .andExpect(jsonPath("$.onTimeRate").value(0.50));
    }

    @Test
    void theReliabilityTableCarriesTheWindowItWasAskedForThroughToTheService() throws Exception {
        when(supplierService.reliability(LocalDate.of(2026, 1, 1))).thenReturn(List.of(
                new SupplierReliabilityResponse(8L, "Bolt Brothers", 1, 0, 1, BigDecimal.ZERO,
                        new BigDecimal("4.0"), 4L, new BigDecimal("90.00"), new BigDecimal("90.00"))));

        mockMvc.perform(get("/api/suppliers/reliability").param("since", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].supplierName").value("Bolt Brothers"));
    }

    @Test
    void reliabilityRejectsAWindowThatIsNotADate() throws Exception {
        mockMvc.perform(get("/api/suppliers/7/reliability").param("since", "last-tuesday"))
                .andExpect(status().isBadRequest());
    }

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
    void createCarriesTheDefaultDeliveryWarehouseThrough() throws Exception {
        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        Supplier supplier = Supplier.builder().id(1L).name("Acme").email("acme@example.com")
                .defaultWarehouse(Warehouse.builder().id(2L).code("WH-SOUTH").build()).build();
        when(supplierService.create(any(Supplier.class))).thenReturn(supplier);

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme","email":"acme@example.com","defaultWarehouse":{"id":2}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.defaultWarehouseId").value(2))
                .andExpect(jsonPath("$.defaultWarehouseCode").value("WH-SOUTH"));

        verify(supplierService).create(captor.capture());
        assertThat(captor.getValue().getDefaultWarehouse().getId()).isEqualTo(2L);
    }

    @Test
    void createCarriesTheLeadTimeThroughAndReportsIt() throws Exception {
        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        Supplier supplier = Supplier.builder().id(1L).name("Acme").email("acme@example.com")
                .leadTimeDays(14).build();
        when(supplierService.create(any(Supplier.class))).thenReturn(supplier);

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme","email":"acme@example.com","leadTimeDays":14}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leadTimeDays").value(14));

        verify(supplierService).create(captor.capture());
        assertThat(captor.getValue().getLeadTimeDays()).isEqualTo(14);
    }

    @Test
    void createRejectsALeadTimeThatIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme","email":"acme@example.com","leadTimeDays":0}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(supplierService);
    }

    @Test
    void createReturnsNotFoundWhenTheDefaultWarehouseDoesNotExist() throws Exception {
        when(supplierService.create(any(Supplier.class)))
                .thenThrow(new ResourceNotFoundException("Warehouse not found with id: 99"));

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme","email":"acme@example.com","defaultWarehouse":{"id":99}}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void findByIdReturnsSupplier() throws Exception {
        Supplier supplier = Supplier.builder().id(1L).name("Acme").contactName("Jane Doe")
                .email("acme@example.com").phone("+1-555-0100").address("1 Industrial Way").build();
        when(supplierService.findById(1L)).thenReturn(supplier);

        mockMvc.perform(get("/api/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"))
                .andExpect(jsonPath("$.contactName").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("acme@example.com"))
                .andExpect(jsonPath("$.phone").value("+1-555-0100"))
                .andExpect(jsonPath("$.address").value("1 Industrial Way"))
                .andExpect(jsonPath("$.defaultWarehouseId").value(nullValue()))
                .andExpect(jsonPath("$.defaultWarehouseCode").value(nullValue()))
                .andExpect(jsonPath("$.leadTimeDays").value(nullValue()));
    }

    @Test
    void responseOmitsProductsAndTenant() throws Exception {
        Supplier supplier = Supplier.builder().id(1L).name("Acme").tenantId("acme").build();
        when(supplierService.findById(1L)).thenReturn(supplier);

        mockMvc.perform(get("/api/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").doesNotExist())
                .andExpect(jsonPath("$.tenantId").doesNotExist());
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
