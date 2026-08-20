package com.example.smartinventory.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.StockAvailabilityResponse;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockLevel;
import com.example.smartinventory.repository.StockReservationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Works out how much of a product is actually free: what is on hand, less what is held by
 * reservations that have not lapsed.
 *
 * <p>The reorder rule and the notification channels both decide on this figure rather than on the
 * quantity on the shelf. Forty units with thirty-eight of them promised is two units of stock and a
 * warehouse that reads full, and a reorder point exists to start the lead time while there is still
 * something left to sell.
 *
 * <p>Holds are counted where they were placed. A hold naming a warehouse counts against that site;
 * one naming none counts against the product total only, which is the same scope rule the reorder
 * point itself follows.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailableStockService {

    private final StockReservationRepository stockReservationRepository;

    /**
     * Measures the product as a whole: everything on hand anywhere, less every unheld hold placed
     * against it, wherever that hold names.
     *
     * @param product the product to measure
     * @return what it breaks down into, with available floored at zero
     */
    public StockAvailabilityResponse measure(Product product) {
        int onHand = product.getQuantity() == null ? 0 : product.getQuantity();
        long reserved = stockReservationRepository.sumHeldForProduct(product.getId(), Instant.now());
        return StockAvailabilityResponse.of(product.getId(), null, onHand, (int) reserved);
    }

    /**
     * Measures one warehouse: what that site holds, less the holds placed against that site. Holds
     * placed against the product without naming a location are not counted here — they are not this
     * site's to answer for, and counting them would make every site look short of the same units.
     *
     * @param product the product to measure
     * @param level   the level of the site to measure it at
     * @return what that site breaks down into, with available floored at zero
     */
    public StockAvailabilityResponse measure(Product product, StockLevel level) {
        Long warehouseId = level.getWarehouse().getId();
        int onHand = level.getQuantity() == null ? 0 : level.getQuantity();
        long reserved = stockReservationRepository
                .sumHeldForProductInWarehouse(product.getId(), warehouseId, Instant.now());
        return StockAvailabilityResponse.of(product.getId(), warehouseId, onHand, (int) reserved);
    }

}
