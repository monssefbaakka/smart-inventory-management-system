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

    /** File name offered to clients downloading the product inventory CSV. */
    static final String CSV_FILENAME = "products.csv";

    private final ReportService reportService;

    @GetMapping("/stock-value")
    public ResponseEntity<BigDecimal> totalStockValue() {
        return ResponseEntity.ok(reportService.totalStockValue());
    }

    /**
     * Streams the full product inventory as a downloadable CSV attachment.
     *
     * @return the CSV document with a {@code text/csv} content type and attachment disposition
     */
    @GetMapping(value = "/products.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportProductsCsv() {
        byte[] body = reportService.exportProductsCsv().getBytes(StandardCharsets.UTF_8);
        ContentDisposition disposition = ContentDisposition.attachment().filename(CSV_FILENAME).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

}
