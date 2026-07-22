package com.example.smartinventory.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

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

    @InjectMocks
    private ReportService reportService;

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
