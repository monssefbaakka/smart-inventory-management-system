package com.example.smartinventory.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.AuditAction;
import com.example.smartinventory.model.Category;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SupplierService supplierService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProductService productService;

    @Test
    void createSavesProduct() {
        Product product = Product.builder().name("Widget").sku("SKU-1").price(BigDecimal.TEN).quantity(5).build();
        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.create(product);

        assertThat(result).isSameAs(product);
        verify(productRepository).save(product);
        verify(auditService).record("Product", product.getId(), AuditAction.CREATE);
    }

    @Test
    void findByIdReturnsProductWhenPresent() {
        Product product = Product.builder().id(1L).name("Widget").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.findById(1L);

        assertThat(result).isSameAs(product);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void findAllReturnsAllProducts() {
        Product product = Product.builder().id(1L).build();
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<Product> result = productService.findAll();

        assertThat(result).containsExactly(product);
    }

    @Test
    void updateAppliesFieldsAndSaves() {
        Product existing = Product.builder()
                .id(1L)
                .name("Old")
                .sku("SKU-OLD")
                .price(BigDecimal.ONE)
                .quantity(1)
                .build();
        Product updated = Product.builder()
                .name("New")
                .sku("SKU-NEW")
                .description("desc")
                .price(BigDecimal.TEN)
                .quantity(9)
                .build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.update(1L, updated);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getSku()).isEqualTo("SKU-NEW");
        assertThat(result.getDescription()).isEqualTo("desc");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.TEN);
        assertThat(result.getQuantity()).isEqualTo(9);
        verify(auditService).record("Product", 1L, AuditAction.UPDATE);
    }

    @Test
    void deleteRemovesExistingProduct() {
        Product existing = Product.builder().id(1L).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.delete(1L);

        verify(productRepository).delete(existing);
        verify(auditService).record("Product", 1L, AuditAction.DELETE);
    }

    @Test
    void findLowStockProductsReturnsBelowThreshold() {
        Product low = Product.builder().id(1L).name("Low").quantity(3).reorderThreshold(10).build();
        when(productRepository.findLowStockProducts()).thenReturn(List.of(low));

        List<Product> result = productService.findLowStockProducts();

        assertThat(result).hasSize(1).containsExactly(low);
        verify(productRepository).findLowStockProducts();
    }

    @Test
    void findByBarcodeReturnsProductWhenPresent() {
        Product product = Product.builder().id(1L).name("Widget").barcode("5901234123457").build();
        when(productRepository.findByBarcode("5901234123457")).thenReturn(Optional.of(product));

        Product result = productService.findByBarcode("5901234123457");

        assertThat(result).isSameAs(product);
    }

    @Test
    void findByBarcodeThrowsWhenMissing() {
        when(productRepository.findByBarcode("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findByBarcode("nope"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void symbolContentPrefersBarcode() {
        Product product = Product.builder().id(1L).sku("SKU-1").barcode("5901234123457").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThat(productService.symbolContent(1L)).isEqualTo("5901234123457");
    }

    @Test
    void symbolContentFallsBackToSkuWhenBarcodeMissing() {
        Product noBarcode = Product.builder().id(1L).sku("SKU-1").build();
        Product blankBarcode = Product.builder().id(2L).sku("SKU-2").barcode("  ").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(noBarcode));
        when(productRepository.findById(2L)).thenReturn(Optional.of(blankBarcode));

        assertThat(productService.symbolContent(1L)).isEqualTo("SKU-1");
        assertThat(productService.symbolContent(2L)).isEqualTo("SKU-2");
    }

    @Test
    void updateAppliesBarcode() {
        Product existing = Product.builder().id(1L).name("A").sku("S").price(BigDecimal.ONE).quantity(1)
                .barcode("old-code").build();
        Product updated = Product.builder().name("A").sku("S").price(BigDecimal.ONE).quantity(1)
                .barcode("5901234123457").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.update(1L, updated);

        assertThat(result.getBarcode()).isEqualTo("5901234123457");
    }

    @Test
    void createReplacesRequestedAssociationsWithPersistedOnes() {
        Category persistedCategory = Category.builder().id(3L).name("Tools").build();
        Supplier persistedSupplier = Supplier.builder().id(4L).name("Acme").build();
        Product product = Product.builder()
                .name("Widget")
                .sku("SKU-1")
                .price(BigDecimal.TEN)
                .quantity(5)
                .category(Category.builder().id(3L).build())
                .supplier(Supplier.builder().id(4L).build())
                .build();
        when(categoryService.findById(3L)).thenReturn(persistedCategory);
        when(supplierService.findById(4L)).thenReturn(persistedSupplier);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.create(product);

        assertThat(result.getCategory()).isSameAs(persistedCategory);
        assertThat(result.getSupplier()).isSameAs(persistedSupplier);
    }

    @Test
    void createLeavesAssociationsUnsetWhenPayloadNamesNone() {
        Product product = Product.builder()
                .name("Widget")
                .sku("SKU-1")
                .price(BigDecimal.TEN)
                .quantity(5)
                .category(Category.builder().build())
                .build();
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.create(product);

        assertThat(result.getCategory()).isNull();
        assertThat(result.getSupplier()).isNull();
        verifyNoInteractions(categoryService, supplierService);
    }

    @Test
    void createFailsWhenNamedCategoryDoesNotExist() {
        Product product = Product.builder()
                .name("Widget")
                .sku("SKU-1")
                .price(BigDecimal.TEN)
                .quantity(5)
                .category(Category.builder().id(99L).build())
                .build();
        when(categoryService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Category not found with id: 99"));

        assertThatThrownBy(() -> productService.create(product))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateResolvesNamedCategory() {
        Category persistedCategory = Category.builder().id(3L).name("Tools").build();
        Product existing = Product.builder().id(1L).name("A").sku("S").price(BigDecimal.ONE).quantity(1).build();
        Product updated = Product.builder().name("A").sku("S").price(BigDecimal.ONE).quantity(1)
                .category(Category.builder().id(3L).build()).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryService.findById(3L)).thenReturn(persistedCategory);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.update(1L, updated);

        assertThat(result.getCategory()).isSameAs(persistedCategory);
    }

    @Test
    void updateSetsReorderThreshold() {
        Product existing = Product.builder().id(1L).name("A").sku("S").price(BigDecimal.ONE).quantity(5)
                .reorderThreshold(5).build();
        Product updated = Product.builder().name("A").sku("S").price(BigDecimal.ONE).quantity(5)
                .reorderThreshold(20).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.update(1L, updated);

        assertThat(result.getReorderThreshold()).isEqualTo(20);
    }

}
