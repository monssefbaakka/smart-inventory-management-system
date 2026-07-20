package com.example.smartinventory.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void exportProductsToCsvIncludesHeaderOnlyWhenNoProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        String csv = reportService.exportProductsToCsv();

        assertThat(csv).isEqualTo("id,sku,name,category,quantity,price,stockValue\n");
    }

    @Test
    void exportProductsToCsvWritesOneRowPerProduct() {
        Category category = Category.builder().id(1L).name("Widgets").build();
        Product product = Product.builder().id(1L).sku("SKU-1").name("Widget").category(category)
                .price(new BigDecimal("10.00")).quantity(3).build();
        when(productRepository.findAll()).thenReturn(List.of(product));

        String csv = reportService.exportProductsToCsv();

        assertThat(csv).isEqualTo("id,sku,name,category,quantity,price,stockValue\n"
                + "1,SKU-1,Widget,Widgets,3,10.00,30.00\n");
    }

    @Test
    void exportProductsToCsvHandlesMissingCategory() {
        Product product = Product.builder().id(1L).sku("SKU-1").name("Widget").category(null)
                .price(new BigDecimal("10.00")).quantity(3).build();
        when(productRepository.findAll()).thenReturn(List.of(product));

        String csv = reportService.exportProductsToCsv();

        assertThat(csv).contains("1,SKU-1,Widget,,3,10.00,30.00");
    }

    @Test
    void exportProductsToCsvEscapesCommasAndQuotesInName() {
        Product product = Product.builder().id(1L).sku("SKU-1").name("Widget, \"Deluxe\"")
                .price(new BigDecimal("10.00")).quantity(1).build();
        when(productRepository.findAll()).thenReturn(List.of(product));

        String csv = reportService.exportProductsToCsv();

        assertThat(csv).contains("\"Widget, \"\"Deluxe\"\"\"");
    }

    @Test
    void exportProductsToCsvWritesMultipleRowsInRepositoryOrder() {
        Product first = Product.builder().id(1L).sku("SKU-1").name("Widget")
                .price(new BigDecimal("10.00")).quantity(1).build();
        Product second = Product.builder().id(2L).sku("SKU-2").name("Gadget")
                .price(new BigDecimal("5.00")).quantity(2).build();
        when(productRepository.findAll()).thenReturn(List.of(first, second));

        String csv = reportService.exportProductsToCsv();

        assertThat(csv).isEqualTo("id,sku,name,category,quantity,price,stockValue\n"
                + "1,SKU-1,Widget,,1,10.00,10.00\n"
                + "2,SKU-2,Gadget,,2,5.00,10.00\n");
    }

    @Test
    void exportStockMovementsToCsvIncludesHeaderOnlyWhenNoMovements() {
        when(stockMovementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))).thenReturn(List.of());

        String csv = reportService.exportStockMovementsToCsv();

        assertThat(csv).isEqualTo("id,productId,productSku,type,quantity,note,createdAt\n");
    }

    @Test
    void exportStockMovementsToCsvWritesOneRowPerMovement() {
        Product product = Product.builder().id(1L).sku("SKU-1").build();
        StockMovement movement = StockMovement.builder().id(1L).product(product).type(MovementType.IN)
                .quantity(5).note("restock").createdAt(Instant.parse("2026-07-20T10:00:00Z")).build();
        when(stockMovementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))).thenReturn(List.of(movement));

        String csv = reportService.exportStockMovementsToCsv();

        assertThat(csv).isEqualTo("id,productId,productSku,type,quantity,note,createdAt\n"
                + "1,1,SKU-1,IN,5,restock,2026-07-20T10:00:00Z\n");
    }

    @Test
    void exportStockMovementsToCsvEscapesCommaInNote() {
        Product product = Product.builder().id(1L).sku("SKU-1").build();
        StockMovement movement = StockMovement.builder().id(1L).product(product).type(MovementType.OUT)
                .quantity(2).note("damaged, written off").createdAt(Instant.parse("2026-07-20T10:00:00Z")).build();
        when(stockMovementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))).thenReturn(List.of(movement));

        String csv = reportService.exportStockMovementsToCsv();

        assertThat(csv).contains("\"damaged, written off\"");
    }

}
