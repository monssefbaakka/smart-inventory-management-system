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

    /** Header row for the product inventory CSV export. */
    static final String CSV_HEADER = "id,sku,name,category,quantity,price,stock_value";

    /** Header row for the stock-movement CSV export. */
    static final String MOVEMENTS_CSV_HEADER = "id,productId,productSku,type,quantity,note,createdAt";

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
