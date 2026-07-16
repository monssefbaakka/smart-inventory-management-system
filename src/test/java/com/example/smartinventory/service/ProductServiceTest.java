package com.example.smartinventory.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createSavesProduct() {
        Product product = Product.builder().name("Widget").sku("SKU-1").price(BigDecimal.TEN).quantity(5).build();
        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.create(product);

        assertThat(result).isSameAs(product);
        verify(productRepository).save(product);
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
    }

    @Test
    void deleteRemovesExistingProduct() {
        Product existing = Product.builder().id(1L).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.delete(1L);

        verify(productRepository).delete(existing);
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
