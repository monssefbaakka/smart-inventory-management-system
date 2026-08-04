package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.CostOfGoodsSoldResponse;
import com.example.smartinventory.dto.InventoryValuationResponse;
import com.example.smartinventory.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    /** File name offered to clients downloading the product inventory Excel workbook. */
    static final String XLSX_FILENAME = "products.xlsx";

    /** Content type for OOXML spreadsheet ({@code .xlsx}) documents. */
    static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** File name offered to clients downloading the product inventory PDF. */
    static final String PDF_FILENAME = "products.pdf";

    private final ReportService reportService;

    @GetMapping("/stock-value")
    @Operation(summary = "Total stock value",
            description = "Returns the sum of price multiplied by quantity across all products, at the "
                    + "selling price. For what that stock cost, see /api/reports/valuation.")
    @ApiResponse(responseCode = "200", description = "Total stock value returned")
    public ResponseEntity<BigDecimal> totalStockValue() {
        return ResponseEntity.ok(reportService.totalStockValue());
    }

    /**
     * Values the inventory at cost: every product's quantity, average cost and extended value, and
     * the total they add up to.
     *
     * @return the inventory valued at cost
     */
    @GetMapping("/valuation")
    @Operation(summary = "Inventory valuation at cost",
            description = "Returns every product's quantity, weighted average cost and extended value, "
                    + "with the total across the catalogue. A product never received at a stated cost "
                    + "carries an average of zero.")
    @ApiResponse(responseCode = "200", description = "Valuation returned")
    public ResponseEntity<InventoryValuationResponse> valuation() {
        return ResponseEntity.ok(reportService.valuation());
    }

    /**
     * Totals what the stock that left over a window cost, optionally for a single product.
     *
     * @param from      start of the window, inclusive; defaults to the beginning of the record
     * @param to        end of the window, exclusive; defaults to now
     * @param productId identifier of a product to narrow the window to, or {@code null}
     * @return the units that left and what they cost
     */
    @GetMapping("/cogs")
    @Operation(summary = "Cost of goods sold",
            description = "Totals the units that left stock over a window and what they cost to acquire, "
                    + "optionally for one product. Outward movements are valued when they are recorded, "
                    + "so a later receipt at a different price does not disturb the figure. The window "
                    + "defaults to the whole record.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cost of goods sold returned"),
        @ApiResponse(responseCode = "400", description = "The window ends before it starts", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<CostOfGoodsSoldResponse> costOfGoodsSold(
            @Parameter(description = "Start of the window, inclusive, as an ISO-8601 instant")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "End of the window, exclusive, as an ISO-8601 instant")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Parameter(description = "Identifier of a product to narrow the window to")
            @RequestParam(required = false) Long productId) {
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now() : to;
        return ResponseEntity.ok(reportService.costOfGoodsSold(start, end, productId));
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

    /**
     * Streams the full product inventory as a downloadable Excel ({@code .xlsx}) attachment.
     *
     * @return the workbook with an OOXML spreadsheet content type and attachment disposition
     */
    @GetMapping(value = "/products.xlsx", produces = XLSX_CONTENT_TYPE)
    @Operation(summary = "Export products as Excel",
            description = "Downloads the full product inventory as an .xlsx attachment.")
    @ApiResponse(responseCode = "200", description = "Excel workbook returned")
    public ResponseEntity<byte[]> exportProductsXlsx() {
        ContentDisposition disposition = ContentDisposition.attachment().filename(XLSX_FILENAME).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(reportService.exportProductsXlsx());
    }

    /**
     * Streams the full product inventory as a downloadable PDF attachment.
     *
     * @return the PDF document with an {@code application/pdf} content type and attachment disposition
     */
    @GetMapping(value = "/products.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Export products as PDF",
            description = "Downloads the full product inventory as an application/pdf attachment.")
    @ApiResponse(responseCode = "200", description = "PDF document returned")
    public ResponseEntity<byte[]> exportProductsPdf() {
        ContentDisposition disposition = ContentDisposition.attachment().filename(PDF_FILENAME).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportService.exportProductsPdf());
    }

    private ResponseEntity<byte[]> csvAttachment(String csv, String filename) {
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

}
