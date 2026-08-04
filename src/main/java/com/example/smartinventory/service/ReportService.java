package com.example.smartinventory.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.CostOfGoodsSoldResponse;
import com.example.smartinventory.dto.InventoryValuationLine;
import com.example.smartinventory.dto.InventoryValuationResponse;
import com.example.smartinventory.exception.InvalidQueryParameterException;
import com.example.smartinventory.model.Category;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.repository.ProductRepository;
import com.example.smartinventory.repository.StockMovementRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;

/** Service computing aggregate inventory reports. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    /** Header row for the product inventory CSV export. */
    static final String CSV_HEADER = "id,sku,name,category,quantity,price,stock_value";

    /** Header row for the stock-movement CSV export. */
    static final String MOVEMENTS_CSV_HEADER = "id,productId,productSku,type,quantity,note,createdAt";

    /** Column headers for the product inventory Excel export. */
    static final String[] XLSX_HEADERS = {"id", "sku", "name", "category", "quantity", "price", "stock_value"};

    /** Column headers for the product inventory PDF export. */
    static final String[] PDF_HEADERS = {"id", "sku", "name", "category", "quantity", "price", "stock_value"};

    private final ProductRepository productRepository;

    private final StockMovementRepository stockMovementRepository;

    private final ProductService productService;

    /**
     * Computes the total retail value of all inventory on hand, summing {@code price * quantity}
     * across every product. What that stock cost is reported by {@link #valuation()} instead.
     *
     * @return the total stock value at the selling price
     */
    public BigDecimal totalStockValue() {
        return productRepository.findAll().stream()
                .map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Values the inventory at what it cost rather than at what it sells for: every product's units
     * on hand multiplied by the weighted average they were acquired at, and the total those lines
     * add up to.
     *
     * <p>A product that has never been received at a stated cost carries an average of zero and is
     * reported as worth nothing, which is the honest answer: what its stock cost is not known.
     *
     * @return every product valued at cost, with the total
     */
    public InventoryValuationResponse valuation() {
        List<InventoryValuationLine> lines = productRepository.findAll().stream()
                .map(InventoryValuationLine::from)
                .toList();
        BigDecimal total = lines.stream()
                .map(InventoryValuationLine::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new InventoryValuationResponse(lines, total);
    }

    /**
     * Totals what the stock that left over a window cost to acquire, optionally for one product.
     *
     * <p>Outward movements are valued as they are recorded, so the figure is the cost of the goods
     * as they were sold and no later receipt at a different price disturbs it.
     *
     * @param from      start of the window, inclusive
     * @param to        end of the window, exclusive
     * @param productId identifier of a product to narrow the window to, or {@code null}
     * @return the units that left and what they cost
     * @throws InvalidQueryParameterException if the window ends before it starts
     * @throws com.example.smartinventory.exception.ResourceNotFoundException if the named product does
     *                                        not exist
     */
    public CostOfGoodsSoldResponse costOfGoodsSold(Instant from, Instant to, Long productId) {
        if (to.isBefore(from)) {
            throw new InvalidQueryParameterException(
                    "The window ends before it starts: from=" + from + " is after to=" + to);
        }
        if (productId == null) {
            return CostOfGoodsSoldResponse.of(from, to, null, stockMovementRepository.sumCostOfGoodsSold(from, to));
        }
        productService.findById(productId);
        return CostOfGoodsSoldResponse.of(from, to, productId,
                stockMovementRepository.sumCostOfGoodsSoldByProduct(productId, from, to));
    }

    /**
     * Renders the full product inventory as an RFC 4180 CSV document. Each row carries the
     * product id, sku, name, category name, quantity, unit price, and computed stock value
     * ({@code price * quantity}).
     *
     * @return the CSV document, header row first
     */
    public String exportProductsCsv() {
        StringBuilder csv = new StringBuilder(CSV_HEADER).append("\r\n");
        for (Product product : productRepository.findAll()) {
            Category category = product.getCategory();
            BigDecimal stockValue = product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity()));
            csv.append(product.getId()).append(',')
                    .append(escape(product.getSku())).append(',')
                    .append(escape(product.getName())).append(',')
                    .append(escape(category == null ? "" : category.getName())).append(',')
                    .append(product.getQuantity()).append(',')
                    .append(product.getPrice().toPlainString()).append(',')
                    .append(stockValue.toPlainString()).append("\r\n");
        }
        return csv.toString();
    }

    /**
     * Renders all stock movements as an RFC 4180 CSV document, most recent first. Each row
     * carries the movement id, product id, product sku, type, quantity, note, and creation
     * timestamp.
     *
     * @return the CSV document, header row first
     */
    public String exportStockMovementsCsv() {
        StringBuilder csv = new StringBuilder(MOVEMENTS_CSV_HEADER).append("\r\n");
        for (StockMovement movement : stockMovementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))) {
            Product product = movement.getProduct();
            csv.append(movement.getId()).append(',')
                    .append(product.getId()).append(',')
                    .append(escape(product.getSku())).append(',')
                    .append(movement.getType().name()).append(',')
                    .append(movement.getQuantity()).append(',')
                    .append(escape(movement.getNote())).append(',')
                    .append(movement.getCreatedAt()).append("\r\n");
        }
        return csv.toString();
    }

    /**
     * Renders the full product inventory as an {@code .xlsx} workbook. The single sheet carries a
     * bold header row followed by one row per product, with quantity, price, and computed stock
     * value ({@code price * quantity}) written as numeric cells.
     *
     * @return the workbook serialized to a byte array
     */
    public byte[] exportProductsXlsx() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Products");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < XLSX_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(XLSX_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Product product : productRepository.findAll()) {
                Category category = product.getCategory();
                BigDecimal stockValue = product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity()));
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(product.getSku());
                row.createCell(2).setCellValue(product.getName());
                row.createCell(3).setCellValue(category == null ? "" : category.getName());
                row.createCell(4).setCellValue(product.getQuantity());
                row.createCell(5).setCellValue(product.getPrice().doubleValue());
                row.createCell(6).setCellValue(stockValue.doubleValue());
            }

            for (int i = 0; i < XLSX_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate product inventory workbook", ex);
        }
    }

    /**
     * Renders the full product inventory as a PDF document: a title followed by a table with a
     * shaded header row and one row per product (id, sku, name, category, quantity, price, and
     * computed stock value, {@code price * quantity}).
     *
     * @return the PDF document serialized to a byte array
     */
    public byte[] exportProductsPdf() {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Product Inventory", titleFont);
            title.setSpacingAfter(12);
            document.add(title);

            PdfPTable table = new PdfPTable(PDF_HEADERS.length);
            table.setWidthPercentage(100);

            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            for (String header : PDF_HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }
            table.setHeaderRows(1);

            for (Product product : productRepository.findAll()) {
                Category category = product.getCategory();
                BigDecimal stockValue = product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity()));
                table.addCell(String.valueOf(product.getId()));
                table.addCell(product.getSku());
                table.addCell(product.getName());
                table.addCell(category == null ? "" : category.getName());
                table.addCell(String.valueOf(product.getQuantity()));
                table.addCell(product.getPrice().toPlainString());
                table.addCell(stockValue.toPlainString());
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate product inventory PDF", ex);
        }
    }

    /**
     * Escapes a value for CSV output per RFC 4180: fields containing a comma, double quote,
     * or line break are wrapped in double quotes with embedded quotes doubled.
     *
     * @param value the raw field value (may be {@code null})
     * @return the escaped field
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

}
