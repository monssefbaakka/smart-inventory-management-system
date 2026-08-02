package com.example.smartinventory.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.StockAvailabilityResponse;
import com.example.smartinventory.dto.StockReservationRequest;
import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.exception.InvalidReservationStateException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.ReservationStatus;
import com.example.smartinventory.model.StockReservation;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.StockReservationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service holding stock against outbound commitments and reporting what is left to promise.
 *
 * <p>A reservation changes no stock: the units stay on the shelf and in the movement history, they
 * simply stop counting as available. They leave only when the reservation is fulfilled, which
 * records the ordinary {@code OUT} movement for them.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StockReservationService {

    private final StockReservationRepository stockReservationRepository;
    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final StockLevelService stockLevelService;
    private final StockMovementService stockMovementService;

    /**
     * Holds stock of a product against a commitment. Naming a warehouse holds the stock of that
     * location and is checked against what it holds; naming none holds against the product total.
     *
     * @param productId identifier of the product to hold
     * @param request   what the stock is held for, how much, where, and until when
     * @return the persisted reservation
     * @throws ResourceNotFoundException  if the product or the named warehouse does not exist
     * @throws InsufficientStockException if less than the requested quantity is available
     */
    public StockReservation reserve(Long productId, StockReservationRequest request) {
        Product product = productService.findById(productId);
        Warehouse warehouse = request.warehouseId() == null
                ? null
                : warehouseService.findById(request.warehouseId());

        StockAvailabilityResponse availability = availabilityOf(product, warehouse);
        if (availability.available() < request.quantity()) {
            throw new InsufficientStockException("Cannot reserve " + request.quantity() + " units of product "
                    + productId + (warehouse == null ? "" : " in warehouse " + warehouse.getCode())
                    + ": only " + availability.available() + " available, with " + availability.reserved()
                    + " of the " + availability.onHand() + " on hand already reserved");
        }

        return stockReservationRepository.save(StockReservation.builder()
                .product(product)
                .warehouse(warehouse)
                .reference(request.reference())
                .quantity(request.quantity())
                .status(ReservationStatus.HELD)
                .expiresAt(request.expiresAt())
                .build());
    }

    @Transactional(readOnly = true)
    public StockReservation findById(Long id) {
        return stockReservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    /**
     * Returns the reservations taken against a product, newest first, optionally narrowed to one
     * lifecycle status.
     *
     * @param productId identifier of the product
     * @param status    the status to report, or {@code null} for every reservation
     * @return the matching reservations
     * @throws ResourceNotFoundException if the product does not exist
     */
    @Transactional(readOnly = true)
    public List<StockReservation> findByProduct(Long productId, ReservationStatus status) {
        productService.findById(productId);
        return status == null
                ? stockReservationRepository.findByProduct(productId)
                : stockReservationRepository.findByProductAndStatus(productId, status);
    }

    /**
     * Reports what a product's stock breaks down into: what is on hand, what of it is reserved, and
     * what is therefore left to promise.
     *
     * @param productId   identifier of the product
     * @param warehouseId identifier of a warehouse to scope the figures to, or {@code null} for the
     *                    product total across every location
     * @return the availability breakdown
     * @throws ResourceNotFoundException if the product or the named warehouse does not exist
     */
    @Transactional(readOnly = true)
    public StockAvailabilityResponse availability(Long productId, Long warehouseId) {
        Product product = productService.findById(productId);
        Warehouse warehouse = warehouseId == null ? null : warehouseService.findById(warehouseId);
        return availabilityOf(product, warehouse);
    }

    /**
     * Gives held stock back without moving anything: the commitment it was taken for is off, so the
     * units count as available again. Releasing a hold that has already lapsed is allowed — it
     * settles the record of a reservation that stopped holding stock when it expired.
     *
     * @param id identifier of the reservation
     * @return the released reservation
     * @throws ResourceNotFoundException        if the reservation does not exist
     * @throws InvalidReservationStateException if it was already released or fulfilled
     */
    public StockReservation release(Long id) {
        StockReservation reservation = findById(id);
        requireHeld(reservation, "released");
        reservation.setStatus(ReservationStatus.RELEASED);
        return stockReservationRepository.save(reservation);
    }

    /**
     * Ships held stock: records the {@code OUT} movement for the reserved quantity against the
     * reserved location and closes the reservation. The stock leaves through the ordinary movement
     * path, so the per-warehouse level, the earliest-expiry-first batch allocation, the low-stock
     * notifications and the automatic reorder all see it as they see any other movement.
     *
     * <p>A lapsed hold cannot be fulfilled: its stock went back to being available when it expired,
     * and shipping it now would take stock that may since have been promised to someone else.
     *
     * @param id identifier of the reservation
     * @return the fulfilled reservation
     * @throws ResourceNotFoundException        if the reservation does not exist
     * @throws InvalidReservationStateException if it was already released or fulfilled, or has lapsed
     * @throws InsufficientStockException       if the stock is no longer there to ship
     */
    public StockReservation fulfil(Long id) {
        StockReservation reservation = findById(id);
        requireHeld(reservation, "fulfilled");
        if (reservation.isExpiredAt(Instant.now())) {
            throw new InvalidReservationStateException("Reservation " + id + " lapsed at "
                    + reservation.getExpiresAt() + " and no longer holds stock; reserve the stock again to ship it");
        }

        Long warehouseId = reservation.getWarehouse() == null ? null : reservation.getWarehouse().getId();
        stockMovementService.record(reservation.getProduct().getId(), warehouseId, null, MovementType.OUT,
                reservation.getQuantity(), "Fulfilled reservation " + reservation.getReference());

        reservation.setStatus(ReservationStatus.FULFILLED);
        return stockReservationRepository.save(reservation);
    }

    /**
     * Works out what is on hand and what of it is spoken for, either for one location or across
     * every one of them.
     *
     * @param product   the product to report on
     * @param warehouse the location to scope the figures to, or {@code null} for the product total
     * @return the availability breakdown
     */
    private StockAvailabilityResponse availabilityOf(Product product, Warehouse warehouse) {
        Instant now = Instant.now();
        if (warehouse == null) {
            int onHand = product.getQuantity() == null ? 0 : product.getQuantity();
            long reserved = stockReservationRepository.sumHeldForProduct(product.getId(), now);
            return StockAvailabilityResponse.of(product.getId(), null, onHand, (int) reserved);
        }

        int onHand = stockLevelService.quantityOnHand(product.getId(), warehouse.getId());
        long reserved = stockReservationRepository
                .sumHeldForProductInWarehouse(product.getId(), warehouse.getId(), now);
        return StockAvailabilityResponse.of(product.getId(), warehouse.getId(), onHand, (int) reserved);
    }

    /**
     * Rejects a step against a reservation that is no longer holding its stock by having been
     * released or fulfilled already.
     *
     * @param reservation the reservation the step was asked of
     * @param step        past participle of the step, for the message
     * @throws InvalidReservationStateException if the reservation is not held
     */
    private void requireHeld(StockReservation reservation, String step) {
        if (reservation.getStatus() != ReservationStatus.HELD) {
            throw new InvalidReservationStateException("Reservation " + reservation.getId() + " is "
                    + reservation.getStatus() + " and cannot be " + step);
        }
    }

}
