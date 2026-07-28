package com.example.smartinventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class StockCountTest {

    @Test
    void onCreateStampsTimesAndDefaultsToDraft() {
        StockCount count = new StockCount();
        count.setStatus(null);

        count.onCreate();

        assertThat(count.getCreatedAt()).isNotNull();
        assertThat(count.getUpdatedAt()).isNotNull();
        assertThat(count.getStatus()).isEqualTo(StockCountStatus.DRAFT);
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        StockCount count = new StockCount();

        count.onUpdate();

        assertThat(count.getUpdatedAt()).isNotNull();
    }

    @Test
    void addLineSetsTheBackReference() {
        StockCount count = StockCount.builder().id(3L).build();
        StockCountLine line = StockCountLine.builder().product(product(1L)).countedQuantity(1).expectedQuantity(1)
                .build();

        count.addLine(line);

        assertThat(count.getLines()).containsExactly(line);
        assertThat(line.getStockCount()).isSameAs(count);
    }

    @Test
    void findLineForProductLocatesAnAlreadyCountedProduct() {
        StockCount count = StockCount.builder().id(3L).build();
        count.addLine(StockCountLine.builder().product(product(1L)).countedQuantity(4).expectedQuantity(4).build());

        assertThat(count.findLineForProduct(1L)).isPresent();
        assertThat(count.findLineForProduct(2L)).isEmpty();
    }

    @Test
    void totalVarianceSumsEveryLine() {
        StockCount count = StockCount.builder().id(3L).build();
        count.addLine(StockCountLine.builder().product(product(1L)).countedQuantity(38).expectedQuantity(42).build());
        count.addLine(StockCountLine.builder().product(product(2L)).countedQuantity(7).expectedQuantity(5).build());

        assertThat(count.getTotalVariance()).isEqualTo(-2);
    }

    @Test
    void totalVarianceIsZeroForAnEmptyCount() {
        assertThat(StockCount.builder().build().getTotalVariance()).isZero();
    }

    private static Product product(Long id) {
        return Product.builder().id(id).build();
    }

}
