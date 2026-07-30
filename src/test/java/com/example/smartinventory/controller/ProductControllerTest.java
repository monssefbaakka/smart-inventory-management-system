package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    void findAllReturnsProducts() throws Exception {
        Product product = Product.builder().id(1L).name("Widget").build();
        when(productService.findAll()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
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
