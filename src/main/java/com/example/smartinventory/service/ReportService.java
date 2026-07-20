package com.example.smartinventory.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.model.Category;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.repository.ProductRepository;
import com.example.smartinventory.repository.StockMovementRepository;

import lombok.RequiredArgsConstructor;

/** Service computing aggregate inventory reports. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final String PRODUCTS_CSV_HEADER = "id,sku,name,category,quantity,price,stockValue";

    private static final String MOVEMENTS_CSV_HEADER = "id,productId,productSku,type,quantity,note,createdAt";

    private final ProductRepository productRepository;

    private final StockMovementRepository stockMovementRepository;

    /**
     * Computes the total value of all inventory on hand, summing {@code price * quantity}
     * across every product.
     *
     * @return the total stock value
     */
    public BigDecimal totalStockValue() {
        return productRepository.findAll().stream()
                .map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Exports all products as CSV: id, sku, name, category, quantity, price, and stock value.
     *
     * @return the CSV document as a single string, including the header row
     */
    public String exportProductsToCsv() {
        StringBuilder csv = new StringBuilder(PRODUCTS_CSV_HEADER).append('\n');
        for (Product product : productRepository.findAll()) {
            csv.append(toCsvRow(product)).append('\n');
        }
        return csv.toString();
    }

    /**
     * Exports all stock movements as CSV, most recent first: id, productId, productSku, type,
     * quantity, note, and createdAt.
     *
     * @return the CSV document as a single string, including the header row
     */
    public String exportStockMovementsToCsv() {
        StringBuilder csv = new StringBuilder(MOVEMENTS_CSV_HEADER).append('\n');
        for (StockMovement movement : stockMovementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))) {
            csv.append(toCsvRow(movement)).append('\n');
        }
        return csv.toString();
    }

    private String toCsvRow(StockMovement movement) {
        return String.join(",",
                String.valueOf(movement.getId()),
                String.valueOf(movement.getProduct().getId()),
                escapeCsv(movement.getProduct().getSku()),
                movement.getType().name(),
                String.valueOf(movement.getQuantity()),
                escapeCsv(movement.getNote()),
                movement.getCreatedAt().toString());
    }

    private String toCsvRow(Product product) {
        Category category = product.getCategory();
        BigDecimal stockValue = product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity()));
        return String.join(",",
                String.valueOf(product.getId()),
                escapeCsv(product.getSku()),
                escapeCsv(product.getName()),
                escapeCsv(category == null ? "" : category.getName()),
                String.valueOf(product.getQuantity()),
                product.getPrice().toPlainString(),
                stockValue.toPlainString());
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

}
