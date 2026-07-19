package com.example.smartinventory.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.service.ReportService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** REST endpoints exposing aggregate inventory reports. */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Aggregate inventory reporting endpoints")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/stock-value")
    public ResponseEntity<BigDecimal> totalStockValue() {
        return ResponseEntity.ok(reportService.totalStockValue());
    }

}
