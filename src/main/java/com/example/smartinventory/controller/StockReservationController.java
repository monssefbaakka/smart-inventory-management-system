package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.StockAvailabilityResponse;
import com.example.smartinventory.dto.StockReservationRequest;
import com.example.smartinventory.dto.StockReservationResponse;
import com.example.smartinventory.model.ReservationStatus;
import com.example.smartinventory.service.StockReservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for holding stock against outbound commitments and reading what is left to promise. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Stock Reservations", description = "Hold stock against commitments and report available stock")
public class StockReservationController {

    private final StockReservationService stockReservationService;

    /**
     * Holds stock of a product against a commitment.
     *
     * @param productId identifier of the product to hold
     * @param request   what the stock is held for, how much, where, and until when
     * @return the created reservation
     */
    @PostMapping("/api/products/{productId}/reservations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reserve stock",
            description = "Holds stock against a commitment so it stops counting as available. Naming a "
                    + "warehouse holds that location's stock; naming none holds against the product total. "
                    + "Nothing moves until the reservation is fulfilled. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Stock reserved"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product or warehouse not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Less than the requested quantity is available",
                content = @Content)
    })
    public ResponseEntity<StockReservationResponse> reserve(
            @Parameter(description = "Identifier of the product") @PathVariable Long productId,
            @Valid @RequestBody StockReservationRequest request) {
        StockReservationResponse created =
                StockReservationResponse.from(stockReservationService.reserve(productId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Returns the reservations taken against a product, newest first.
     *
     * @param productId identifier of the product
     * @param status    lifecycle status to filter by, or {@code null} for every reservation
     * @return the product's reservations
     */
    @GetMapping("/api/products/{productId}/reservations")
    @Operation(summary = "List a product's reservations",
            description = "Returns the reservations taken against the product, newest first, optionally "
                    + "narrowed to one lifecycle status.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservations returned"),
        @ApiResponse(responseCode = "400", description = "Unknown status", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<List<StockReservationResponse>> findByProduct(
            @Parameter(description = "Identifier of the product") @PathVariable Long productId,
            @Parameter(description = "Only reservations in this lifecycle status")
            @RequestParam(required = false) ReservationStatus status) {
        return ResponseEntity.ok(stockReservationService.findByProduct(productId, status).stream()
                .map(StockReservationResponse::from)
                .toList());
    }

    /**
     * Reports what a product's stock breaks down into.
     *
     * @param productId   identifier of the product
     * @param warehouseId identifier of a warehouse to scope the figures to, or {@code null}
     * @return on hand, reserved and available stock
     */
    @GetMapping("/api/products/{productId}/availability")
    @Operation(summary = "Report available stock",
            description = "Returns what is on hand, what of it is reserved, and what is left to promise. "
                    + "Naming a warehouse scopes every figure to that location; without one they cover the "
                    + "product total across every location.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Availability returned"),
        @ApiResponse(responseCode = "404", description = "Product or warehouse not found", content = @Content)
    })
    public ResponseEntity<StockAvailabilityResponse> availability(
            @Parameter(description = "Identifier of the product") @PathVariable Long productId,
            @Parameter(description = "Only the stock held in this warehouse")
            @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(stockReservationService.availability(productId, warehouseId));
    }

    @GetMapping("/api/reservations/{id}")
    @Operation(summary = "Get a reservation by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation found"),
        @ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content)
    })
    public ResponseEntity<StockReservationResponse> findById(
            @Parameter(description = "Identifier of the reservation") @PathVariable Long id) {
        return ResponseEntity.ok(StockReservationResponse.from(stockReservationService.findById(id)));
    }

    /**
     * Gives held stock back without moving anything.
     *
     * @param id identifier of the reservation
     * @return the released reservation
     */
    @PostMapping("/api/reservations/{id}/release")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Release a reservation",
            description = "Gives the held stock back so it counts as available again. Nothing moves. "
                    + "Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation released"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Reservation was already released or fulfilled",
                content = @Content)
    })
    public ResponseEntity<StockReservationResponse> release(
            @Parameter(description = "Identifier of the reservation") @PathVariable Long id) {
        return ResponseEntity.ok(StockReservationResponse.from(stockReservationService.release(id)));
    }

    /**
     * Ships held stock, recording the outward movement for it.
     *
     * @param id identifier of the reservation
     * @return the fulfilled reservation
     */
    @PostMapping("/api/reservations/{id}/fulfil")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fulfil a reservation",
            description = "Records the OUT movement for the reserved quantity against the reserved location "
                    + "and closes the reservation, so the stock leaves through the ordinary movement trail. "
                    + "Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation fulfilled"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Reservation is not held any more, has lapsed, or the "
                + "stock is no longer there to ship", content = @Content)
    })
    public ResponseEntity<StockReservationResponse> fulfil(
            @Parameter(description = "Identifier of the reservation") @PathVariable Long id) {
        return ResponseEntity.ok(StockReservationResponse.from(stockReservationService.fulfil(id)));
    }

}
