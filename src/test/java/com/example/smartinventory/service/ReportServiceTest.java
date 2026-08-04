package com.example.smartinventory.service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.example.smartinventory.dto.CostOfGoodsSoldResponse;
import com.example.smartinventory.dto.CostOfGoodsSoldTotals;
import com.example.smartinventory.dto.InventoryValuationResponse;
import com.example.smartinventory.exception.InvalidQueryParameterException;
import com.example.smartinventory.model.Category;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.repository.ProductRepository;
import com.example.smartinventory.repository.StockMovementRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ReportService reportService;

    @Test
    void valuationValuesEveryProductAtItsAverageCostAndTotalsTheLines() {
        Product a = Product.builder().id(1L).sku("SKU-1").name("Hammer").quantity(3)
                .averageCost(new BigDecimal("10.0000")).build();
        Product b = Product.builder().id(2L).sku("SKU-2").name("Nail").quantity(4)
                .averageCost(new BigDecimal("2.5000")).build();
        when(productRepository.findAll()).thenReturn(List.of(a, b));

        InventoryValuationResponse result = reportService.valuation();

        assertThat(result.products()).hasSize(2);
        assertThat(result.products().get(0).value()).isEqualByComparingTo("30.0000");
        assertThat(result.products().get(0).sku()).isEqualTo("SKU-1");
        assertThat(result.total()).isEqualByComparingTo("40.0000");
    }

    @Test
    void valuationReportsAProductNeverReceivedAtAStatedCostAsWorthNothing() {
        Product product = Product.builder().id(1L).sku("SKU-1").name("Hammer").quantity(9).build();
        when(productRepository.findAll()).thenReturn(List.of(product));

        InventoryValuationResponse result = reportService.valuation();

        assertThat(result.products().get(0).averageCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void costOfGoodsSoldTotalsTheOutwardMovementsOfTheWindow() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        when(stockMovementRepository.sumCostOfGoodsSold(from, to))
                .thenReturn(new CostOfGoodsSoldTotals(120L, new BigDecimal("600.0000")));

        CostOfGoodsSoldResponse result = reportService.costOfGoodsSold(from, to, null);

        assertThat(result.quantity()).isEqualTo(120L);
        assertThat(result.totalCost()).isEqualByComparingTo("600.0000");
        assertThat(result.productId()).isNull();
    }

    @Test
    void costOfGoodsSoldNarrowsToOneProductAndChecksItExists() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        when(stockMovementRepository.sumCostOfGoodsSoldByProduct(5L, from, to))
                .thenReturn(new CostOfGoodsSoldTotals(4L, new BigDecimal("18.0000")));

        CostOfGoodsSoldResponse result = reportService.costOfGoodsSold(from, to, 5L);

        assertThat(result.productId()).isEqualTo(5L);
        assertThat(result.totalCost()).isEqualByComparingTo("18.0000");
        verify(productService).findById(5L);
    }

    @Test
    void costOfGoodsSoldReadsAWindowWithoutMovementsAsZero() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        when(stockMovementRepository.sumCostOfGoodsSold(from, to))
                .thenReturn(new CostOfGoodsSoldTotals(null, null));

        CostOfGoodsSoldResponse result = reportService.costOfGoodsSold(from, to, null);

        assertThat(result.quantity()).isZero();
        assertThat(result.totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void costOfGoodsSoldRejectsAWindowThatEndsBeforeItStarts() {
        Instant from = Instant.parse("2026-02-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z");

        assertThatThrownBy(() -> reportService.costOfGoodsSold(from, to, null))
                .isInstanceOf(InvalidQueryParameterException.class);

        verifyNoInteractions(stockMovementRepository, productService);
    }

    @Test
    void totalStockValueSumsPriceTimesQuantity() {
        Product a = Product.builder().id(1L).price(new BigDecimal("10.00")).quantity(3).build();
        Product b = Product.builder().id(2L).price(new BigDecimal("2.50")).quantity(4).build();
        when(productRepository.findAll()).thenReturn(List.of(a, b));

        BigDecimal result = reportService.totalStockValue();

        assertThat(result).isEqualByComparingTo("40.00");
    }

    @Test
    void totalStockValueReturnsZeroWhenNoProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        BigDecimal result = reportService.totalStockValue();

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void exportProductsCsvWritesHeaderOnlyWhenNoProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        String csv = reportService.exportProductsCsv();

        assertThat(csv).isEqualTo("id,sku,name,category,quantity,price,stock_value\r\n");
    }

    @Test
    void exportProductsCsvWritesRowPerProductWithComputedStockValue() {
        Category tools = Category.builder().name("Tools").build();
        Product a = Product.builder().id(1L).sku("SKU-1").name("Hammer")
                .category(tools).price(new BigDecimal("10.00")).quantity(3).build();
        Product b = Product.builder().id(2L).sku("SKU-2").name("Nail")
                .price(new BigDecimal("2.50")).quantity(4).build();
        when(productRepository.findAll()).thenReturn(List.of(a, b));

        String csv = reportService.exportProductsCsv();

        assertThat(csv).isEqualTo("id,sku,name,category,quantity,price,stock_value\r\n"
                + "1,SKU-1,Hammer,Tools,3,10.00,30.00\r\n"
                + "2,SKU-2,Nail,,4,2.50,10.00\r\n");
    }

    @Test
    void exportProductsPdfProducesWellFormedPdfWhenNoProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        byte[] pdf = reportService.exportProductsPdf();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("%%EOF");
    }

    @Test
    void exportProductsPdfProducesWellFormedPdfWithProducts() {
        Category tools = Category.builder().name("Tools").build();
        Product a = Product.builder().id(1L).sku("SKU-1").name("Hammer")
                .category(tools).price(new BigDecimal("10.00")).quantity(3).build();
        Product b = Product.builder().id(2L).sku("SKU-2").name("Nail")
                .price(new BigDecimal("2.50")).quantity(4).build();
        when(productRepository.findAll()).thenReturn(List.of(a, b));

        byte[] pdf = reportService.exportProductsPdf();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("%%EOF");
    }

    @Test
    void exportProductsCsvEscapesFieldsContainingCommasAndQuotes() {
        Category odd = Category.builder().name("Power, Tools").build();
        Product a = Product.builder().id(1L).sku("SKU-1").name("12\" \"Wrench\"")
                .category(odd).price(new BigDecimal("5.00")).quantity(1).build();
        when(productRepository.findAll()).thenReturn(List.of(a));

        String csv = reportService.exportProductsCsv();

        assertThat(csv).isEqualTo("id,sku,name,category,quantity,price,stock_value\r\n"
                + "1,SKU-1,\"12\"\" \"\"Wrench\"\"\",\"Power, Tools\",1,5.00,5.00\r\n");
    }

    @Test
    void exportProductsXlsxWritesHeaderOnlyWhenNoProducts() throws Exception {
        when(productRepository.findAll()).thenReturn(List.of());

        byte[] bytes = reportService.exportProductsXlsx();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Products");
            assertThat(sheet.getLastRowNum()).isZero();
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("id");
            assertThat(header.getCell(6).getStringCellValue()).isEqualTo("stock_value");
        }
    }

    @Test
    void exportProductsXlsxWritesRowPerProductWithNumericCells() throws Exception {
        Category tools = Category.builder().name("Tools").build();
        Product a = Product.builder().id(1L).sku("SKU-1").name("Hammer")
                .category(tools).price(new BigDecimal("10.00")).quantity(3).build();
        Product b = Product.builder().id(2L).sku("SKU-2").name("Nail")
                .price(new BigDecimal("2.50")).quantity(4).build();
        when(productRepository.findAll()).thenReturn(List.of(a, b));

        byte[] bytes = reportService.exportProductsXlsx();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(2);

            Row first = sheet.getRow(1);
            assertThat(first.getCell(0).getNumericCellValue()).isEqualTo(1.0);
            assertThat(first.getCell(1).getStringCellValue()).isEqualTo("SKU-1");
            assertThat(first.getCell(2).getStringCellValue()).isEqualTo("Hammer");
            assertThat(first.getCell(3).getStringCellValue()).isEqualTo("Tools");
            assertThat(first.getCell(4).getNumericCellValue()).isEqualTo(3.0);
            assertThat(first.getCell(5).getNumericCellValue()).isEqualTo(10.0);
            assertThat(first.getCell(6).getNumericCellValue()).isEqualTo(30.0);

            Row second = sheet.getRow(2);
            assertThat(second.getCell(3).getStringCellValue()).isEmpty();
            assertThat(second.getCell(6).getNumericCellValue()).isEqualTo(10.0);
        }
    }

    @Test
    void exportStockMovementsCsvWritesHeaderOnlyWhenNoMovements() {
        when(stockMovementRepository.findAll(any(Sort.class))).thenReturn(List.of());

        String csv = reportService.exportStockMovementsCsv();

        assertThat(csv).isEqualTo("id,productId,productSku,type,quantity,note,createdAt\r\n");
    }

    @Test
    void exportStockMovementsCsvWritesRowPerMovement() {
        Product product = Product.builder().id(3L).sku("SKU-3").build();
        Instant when = Instant.parse("2026-01-02T03:04:05Z");
        StockMovement movement = StockMovement.builder().id(9L).product(product).type(MovementType.IN)
                .quantity(5).note("restock").createdAt(when).build();
        when(stockMovementRepository.findAll(any(Sort.class))).thenReturn(List.of(movement));

        String csv = reportService.exportStockMovementsCsv();

        assertThat(csv).isEqualTo("id,productId,productSku,type,quantity,note,createdAt\r\n"
                + "9,3,SKU-3,IN,5,restock,2026-01-02T03:04:05Z\r\n");
    }

    @Test
    void exportStockMovementsCsvEscapesNoteAndHandlesNullNote() {
        Product product = Product.builder().id(3L).sku("SKU-3").build();
        Instant when = Instant.parse("2026-01-02T03:04:05Z");
        StockMovement withComma = StockMovement.builder().id(1L).product(product).type(MovementType.OUT)
                .quantity(2).note("sold, urgent").createdAt(when).build();
        StockMovement noNote = StockMovement.builder().id(2L).product(product).type(MovementType.ADJUSTMENT)
                .quantity(7).createdAt(when).build();
        when(stockMovementRepository.findAll(any(Sort.class))).thenReturn(List.of(withComma, noNote));

        String csv = reportService.exportStockMovementsCsv();

        assertThat(csv).isEqualTo("id,productId,productSku,type,quantity,note,createdAt\r\n"
                + "1,3,SKU-3,OUT,2,\"sold, urgent\",2026-01-02T03:04:05Z\r\n"
                + "2,3,SKU-3,ADJUSTMENT,7,,2026-01-02T03:04:05Z\r\n");
    }

}
