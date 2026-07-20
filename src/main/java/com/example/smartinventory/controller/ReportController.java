package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    /**
     * Downloads all products as a CSV attachment.
     *
     * @return the CSV file as an octet response with a {@code Content-Disposition} header
     */
    @GetMapping("/export/products")
    public ResponseEntity<byte[]> exportProductsCsv() {
        return csvAttachment(reportService.exportProductsToCsv(), "products.csv");
    }

    /**
     * Downloads all stock movements as a CSV attachment, most recent first.
     *
     * @return the CSV file as an octet response with a {@code Content-Disposition} header
     */
    @GetMapping("/export/movements")
    public ResponseEntity<byte[]> exportStockMovementsCsv() {
        return csvAttachment(reportService.exportStockMovementsToCsv(), "movements.csv");
    }

    private ResponseEntity<byte[]> csvAttachment(String csv, String filename) {
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

}
