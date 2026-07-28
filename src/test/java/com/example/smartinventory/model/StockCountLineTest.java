package com.example.smartinventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class StockCountLineTest {

    @Test
    void varianceIsNegativeWhenStockIsMissing() {
        assertThat(line(38, 42).getVariance()).isEqualTo(-4);
    }

    @Test
    void varianceIsPositiveWhenTheShelfHeldMore() {
        assertThat(line(7, 5).getVariance()).isEqualTo(2);
    }

    @Test
    void varianceIsZeroWhenTheCountMatches() {
        assertThat(line(12, 12).getVariance()).isZero();
    }

    private static StockCountLine line(int counted, int expected) {
        return StockCountLine.builder().countedQuantity(counted).expectedQuantity(expected).build();
    }

}
