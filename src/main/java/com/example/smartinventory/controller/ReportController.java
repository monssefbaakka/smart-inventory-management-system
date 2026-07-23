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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    /** File name offered to clients downloading the stock-movement CSV. */
    static final String MOVEMENTS_CSV_FILENAME = "movements.csv";

    private final ReportService reportService;

    @GetMapping("/stock-value")
    @Operation(summary = "Total stock value",
            description = "Returns the sum of price multiplied by quantity across all products.")
    @ApiResponse(responseCode = "200", description = "Total stock value returned")
    public ResponseEntity<BigDecimal> totalStockValue() {
        return ResponseEntity.ok(reportService.totalStockValue());
    }

    /**
     * Streams the full product inventory as a downloadable CSV attachment.
     *
     * @return the CSV document with a {@code text/csv} content type and attachment disposition
     */
    @GetMapping(value = "/products.csv", produces = "text/csv")
    @Operation(summary = "Export products as CSV",
            description = "Downloads the full product inventory as a text/csv attachment.")
    @ApiResponse(responseCode = "200", description = "CSV document returned")
    public ResponseEntity<byte[]> exportProductsCsv() {
        return csvAttachment(reportService.exportProductsCsv(), CSV_FILENAME);
    }

    /**
     * Streams all stock movements as a downloadable CSV attachment, most recent first.
     *
     * @return the CSV document with a {@code text/csv} content type and attachment disposition
     */
    @GetMapping(value = "/export/movements", produces = "text/csv")
    @Operation(summary = "Export stock movements as CSV",
            description = "Downloads all stock movements (most recent first) as a text/csv attachment.")
    @ApiResponse(responseCode = "200", description = "CSV document returned")
    public ResponseEntity<byte[]> exportStockMovementsCsv() {
        return csvAttachment(reportService.exportStockMovementsCsv(), MOVEMENTS_CSV_FILENAME);
    }

    private ResponseEntity<byte[]> csvAttachment(String csv, String filename) {
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

}
