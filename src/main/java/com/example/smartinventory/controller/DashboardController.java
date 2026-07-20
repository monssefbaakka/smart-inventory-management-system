package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.DashboardSummaryResponse;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.service.DashboardService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** REST endpoints exposing the inventory dashboard: aggregate counts and recent activity. */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregate inventory dashboard endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> summary() {
        return ResponseEntity.ok(dashboardService.summary());
    }

    @GetMapping("/recent-movements")
    public ResponseEntity<List<StockMovement>> recentMovements(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.recentMovements(limit));
    }

}
