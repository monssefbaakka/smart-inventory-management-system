package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.PurchaseOrderRequest;
import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.service.PurchaseOrderService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for managing purchase orders and their lifecycle. */
@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Orders", description = "Create purchase orders and drive their lifecycle")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PurchaseOrder> create(@Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrder created = purchaseOrderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> findById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.findById(id));
    }

    /**
     * Lists purchase orders, optionally filtered to a single supplier.
     *
     * @param supplierId optional supplier filter
     * @return the matching purchase orders
     */
    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> findAll(@RequestParam(required = false) Long supplierId) {
        List<PurchaseOrder> orders = supplierId == null
                ? purchaseOrderService.findAll()
                : purchaseOrderService.findBySupplier(supplierId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/place")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PurchaseOrder> place(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.place(id));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PurchaseOrder> receive(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.receive(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PurchaseOrder> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.cancel(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        purchaseOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
