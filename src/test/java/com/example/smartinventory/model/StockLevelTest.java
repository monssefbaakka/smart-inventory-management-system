package com.example.smartinventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class StockLevelTest {

    @Test
    void onSaveSetsUpdatedAt() {
        StockLevel level = new StockLevel();

        level.onSave();

        assertThat(level.getUpdatedAt()).isNotNull();
    }

    @Test
    void builderDefaultsQuantityToZero() {
        assertThat(StockLevel.builder().build().getQuantity()).isZero();
    }

}
