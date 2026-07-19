package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.StockMovementRequest;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.service.StockMovementService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for recording and viewing product stock movement history. */
@RestController
@RequestMapping("/api/products/{productId}/movements")
@RequiredArgsConstructor
@Tag(name = "Stock Movements", description = "Record and view stock movement history for products")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockMovement> record(@PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request) {
        StockMovement movement = stockMovementService.record(productId, request.type(), request.quantity(),
                request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(movement);
    }

    @GetMapping
    public ResponseEntity<List<StockMovement>> findByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockMovementService.findByProduct(productId));
    }

}
