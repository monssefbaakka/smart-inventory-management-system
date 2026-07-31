package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.smartinventory.dto.ProductSearchCriteria;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Category;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.security.JwtService;
import com.example.smartinventory.security.UserDetailsServiceImpl;
import com.example.smartinventory.service.BarcodeService;
import com.example.smartinventory.service.ProductService;

@WebMvcTest(controllers = ProductController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private BarcodeService barcodeService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void createReturnsCreatedProduct() throws Exception {
        Product product = Product.builder().id(1L).name("Widget").sku("SKU-1").price(BigDecimal.TEN).quantity(3)
                .build();
        when(productService.create(any(Product.class))).thenReturn(product);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Widget","sku":"SKU-1","price":10,"quantity":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findByIdReturnsProduct() throws Exception {
        Product product = Product.builder().id(1L).name("Widget").build();
        when(productService.findById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Widget"));
    }

    @Test
    void findByIdFlattensCategoryAndSupplier() throws Exception {
        Product product = Product.builder()
                .id(1L)
                .name("Widget")
                .tenantId("acme")
                .category(Category.builder().id(3L).name("Tools").build())
                .supplier(Supplier.builder().id(4L).name("Acme Supplies").build())
                .build();
        when(productService.findById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(3))
                .andExpect(jsonPath("$.categoryName").value("Tools"))
                .andExpect(jsonPath("$.supplierId").value(4))
                .andExpect(jsonPath("$.supplierName").value("Acme Supplies"))
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.supplier").doesNotExist())
                .andExpect(jsonPath("$.tenantId").doesNotExist());
    }

    @Test
    void findByIdOmitsCategoryAndSupplierWhenUnset() throws Exception {
        Product product = Product.builder().id(1L).name("Widget").build();
        when(productService.findById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").doesNotExist())
                .andExpect(jsonPath("$.supplierName").doesNotExist());
    }

    @Test
    void findByIdReturnsNotFoundWhenMissing() throws Exception {
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchReturnsAPageOfProducts() throws Exception {
        Product product = Product.builder().id(1L).name("Widget").build();
        when(productService.search(any(ProductSearchCriteria.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void searchDefaultsToTheFirstPageOrderedById() throws Exception {
        when(productService.search(any(ProductSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        assertThat(capturedPageable()).isEqualTo(PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));
    }

    @Test
    void searchHonoursThePagingAndSortingAsked() throws Exception {
        when(productService.search(any(ProductSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/products").param("page", "2").param("size", "5").param("sort", "price,desc"))
                .andExpect(status().isOk());

        assertThat(capturedPageable()).isEqualTo(PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "price")));
    }

    @Test
    void searchSortsAscendingWhenNoDirectionIsGiven() throws Exception {
        when(productService.search(any(ProductSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/products").param("sort", "name"))
                .andExpect(status().isOk());

        assertThat(capturedPageable().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Test
    void searchPassesEveryFilterThrough() throws Exception {
        when(productService.search(any(ProductSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/products")
                        .param("search", "wid")
                        .param("categoryId", "3")
                        .param("supplierId", "4")
                        .param("minPrice", "5.50")
                        .param("maxPrice", "50")
                        .param("lowStock", "true"))
                .andExpect(status().isOk());

        assertThat(capturedCriteria()).isEqualTo(
                new ProductSearchCriteria("wid", 3L, 4L, new BigDecimal("5.50"), new BigDecimal("50"), true));
    }

    @Test
    void searchAppliesNoFilterWhenNoneIsGiven() throws Exception {
        when(productService.search(any(ProductSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        assertThat(capturedCriteria()).isEqualTo(ProductSearchCriteria.UNFILTERED);
    }

    @Test
    void searchRejectsAnUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "tenantId"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot sort by 'tenantId'")));

        verifyNoInteractions(productService);
    }

    @Test
    void searchRejectsAnUnknownSortDirection() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "name,sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("sideways")));

        verifyNoInteractions(productService);
    }

    @Test
    void searchRejectsANegativePage() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("page must not be negative")));

        verifyNoInteractions(productService);
    }

    @Test
    void searchRejectsAPageSizeBeyondTheCap() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("size must be between 1 and 100")));

        verifyNoInteractions(productService);
    }

    @Test
    void searchRejectsAnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.captor();
        verify(productService).search(any(ProductSearchCriteria.class), pageable.capture());
        return pageable.getValue();
    }

    private ProductSearchCriteria capturedCriteria() {
        ArgumentCaptor<ProductSearchCriteria> criteria = ArgumentCaptor.captor();
        verify(productService).search(criteria.capture(), any(Pageable.class));
        return criteria.getValue();
    }

    @Test
    void updateReturnsUpdatedProduct() throws Exception {
        Product product = Product.builder().id(1L).name("Updated").sku("SKU-1").price(BigDecimal.TEN).quantity(3)
                .build();
        when(productService.update(eq(1L), any(Product.class))).thenReturn(product);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated","sku":"SKU-1","price":10,"quantity":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).delete(1L);
    }

    @Test
    void findByBarcodeReturnsProduct() throws Exception {
        Product product = Product.builder().id(1L).name("Widget").barcode("5901234123457").build();
        when(productService.findByBarcode("5901234123457")).thenReturn(product);

        mockMvc.perform(get("/api/products/barcode/5901234123457"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("5901234123457"));
    }

    @Test
    void findByBarcodeReturnsNotFoundWhenUnknown() throws Exception {
        when(productService.findByBarcode("unknown"))
                .thenThrow(new ResourceNotFoundException("Product not found with barcode: unknown"));

        mockMvc.perform(get("/api/products/barcode/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void barcodeImageReturnsPng() throws Exception {
        byte[] png = {1, 2, 3};
        when(productService.symbolContent(1L)).thenReturn("SKU-1");
        when(barcodeService.generateBarcode("SKU-1")).thenReturn(png);

        mockMvc.perform(get("/api/products/1/barcode.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png));
    }

    @Test
    void qrCodeImageReturnsPng() throws Exception {
        byte[] png = {4, 5, 6};
        when(productService.symbolContent(1L)).thenReturn("SKU-1");
        when(barcodeService.generateQrCode("SKU-1")).thenReturn(png);

        mockMvc.perform(get("/api/products/1/qrcode.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png));
    }

    @Test
    void barcodeImageReturnsNotFoundWhenProductMissing() throws Exception {
        when(productService.symbolContent(99L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99/barcode.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findLowStockReturnsOkWithProducts() throws Exception {
        Product low = Product.builder().id(2L).name("Low Widget").sku("SKU-L").price(BigDecimal.ONE)
                .quantity(2).reorderThreshold(10).build();
        when(productService.findLowStockProducts()).thenReturn(List.of(low));

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Low Widget"));
    }

}
