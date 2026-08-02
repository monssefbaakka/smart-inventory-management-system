package com.example.smartinventory.controller;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import com.example.smartinventory.dto.StockAvailabilityResponse;
import com.example.smartinventory.dto.StockReservationRequest;
import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.exception.InvalidReservationStateException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.ReservationStatus;
import com.example.smartinventory.model.StockReservation;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.StockReservationService;

@WebMvcTest(controllers = StockReservationController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class StockReservationControllerTest {

    private static final Product PRODUCT = Product.builder().id(1L).sku("SKU-1").name("Widget").quantity(40).build();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockReservationService stockReservationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void reserveReturnsTheHeldStock() throws Exception {
        StockReservation reservation = held(3L);
        reservation.setWarehouse(Warehouse.builder().id(7L).code("WH-1").build());
        when(stockReservationService.reserve(eq(1L), any(StockReservationRequest.class))).thenReturn(reservation);

        mockMvc.perform(post("/api/products/1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"SO-1042","quantity":12,"warehouseId":7}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.reference").value("SO-1042"))
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.expired").value(false))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productSku").value("SKU-1"))
                .andExpect(jsonPath("$.warehouseId").value(7))
                .andExpect(jsonPath("$.warehouseCode").value("WH-1"))
                .andExpect(jsonPath("$.product").doesNotExist());
    }

    @Test
    void reserveRejectsABlankReference() throws Exception {
        mockMvc.perform(post("/api/products/1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"  ","quantity":12}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reserveRejectsAQuantityThatHoldsNothing() throws Exception {
        mockMvc.perform(post("/api/products/1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"SO-1042","quantity":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reserveRejectsAnExpiryInThePast() throws Exception {
        mockMvc.perform(post("/api/products/1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"SO-1042","quantity":12,"expiresAt":"2020-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reserveReportsTooLittleAvailableStock() throws Exception {
        when(stockReservationService.reserve(eq(1L), any(StockReservationRequest.class)))
                .thenThrow(new InsufficientStockException("Cannot reserve 12 units of product 1"));

        mockMvc.perform(post("/api/products/1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference":"SO-1042","quantity":12}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void findByProductReturnsTheReservations() throws Exception {
        when(stockReservationService.findByProduct(1L, null)).thenReturn(List.of(held(3L)));

        mockMvc.perform(get("/api/products/1/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].reference").value("SO-1042"))
                .andExpect(jsonPath("$[0].warehouseId").doesNotExist());
    }

    @Test
    void findByProductPassesTheStatusFilterOn() throws Exception {
        when(stockReservationService.findByProduct(1L, ReservationStatus.FULFILLED)).thenReturn(List.of());

        mockMvc.perform(get("/api/products/1/reservations").param("status", "FULFILLED"))
                .andExpect(status().isOk());

        verify(stockReservationService).findByProduct(1L, ReservationStatus.FULFILLED);
    }

    @Test
    void findByProductRejectsAnUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/products/1/reservations").param("status", "PENDING"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findByProductReportsAnUnknownProduct() throws Exception {
        when(stockReservationService.findByProduct(9L, null))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 9"));

        mockMvc.perform(get("/api/products/9/reservations"))
                .andExpect(status().isNotFound());
    }

    @Test
    void availabilityReturnsTheBreakdown() throws Exception {
        when(stockReservationService.availability(eq(1L), isNull()))
                .thenReturn(StockAvailabilityResponse.of(1L, null, 40, 12));

        mockMvc.perform(get("/api/products/1/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.onHand").value(40))
                .andExpect(jsonPath("$.reserved").value(12))
                .andExpect(jsonPath("$.available").value(28));
    }

    @Test
    void availabilityScopesToTheNamedWarehouse() throws Exception {
        when(stockReservationService.availability(1L, 7L))
                .thenReturn(StockAvailabilityResponse.of(1L, 7L, 20, 5));

        mockMvc.perform(get("/api/products/1/availability").param("warehouseId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseId").value(7))
                .andExpect(jsonPath("$.available").value(15));
    }

    @Test
    void findByIdReportsAnUnknownReservation() throws Exception {
        when(stockReservationService.findById(9L))
                .thenThrow(new ResourceNotFoundException("Reservation not found with id: 9"));

        mockMvc.perform(get("/api/reservations/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void releaseReturnsTheReleasedReservation() throws Exception {
        StockReservation reservation = held(3L);
        reservation.setStatus(ReservationStatus.RELEASED);
        when(stockReservationService.release(3L)).thenReturn(reservation);

        mockMvc.perform(post("/api/reservations/3/release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));
    }

    @Test
    void releaseReportsAReservationThatIsNoLongerHeld() throws Exception {
        when(stockReservationService.release(3L))
                .thenThrow(new InvalidReservationStateException("Reservation 3 is FULFILLED and cannot be released"));

        mockMvc.perform(post("/api/reservations/3/release"))
                .andExpect(status().isConflict());
    }

    @Test
    void fulfilReturnsTheFulfilledReservation() throws Exception {
        StockReservation reservation = held(3L);
        reservation.setStatus(ReservationStatus.FULFILLED);
        when(stockReservationService.fulfil(3L)).thenReturn(reservation);

        mockMvc.perform(post("/api/reservations/3/fulfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));
    }

    @Test
    void fulfilReportsAHoldThatHasLapsed() throws Exception {
        when(stockReservationService.fulfil(3L))
                .thenThrow(new InvalidReservationStateException("Reservation 3 lapsed at 2026-08-01T12:00:00Z"));

        mockMvc.perform(post("/api/reservations/3/fulfil"))
                .andExpect(status().isConflict());
    }

    private static StockReservation held(Long id) {
        return StockReservation.builder()
                .id(id)
                .product(PRODUCT)
                .reference("SO-1042")
                .quantity(12)
                .status(ReservationStatus.HELD)
                .createdAt(Instant.parse("2026-08-02T10:00:00Z"))
                .updatedAt(Instant.parse("2026-08-02T10:00:00Z"))
                .build();
    }

}
