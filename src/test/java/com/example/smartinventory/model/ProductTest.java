package com.example.smartinventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void onCreateSetsTimestamps() {
        Product product = new Product();

        product.onCreate();

        assertThat(product.getCreatedAt()).isNotNull();
        assertThat(product.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        Product product = new Product();
        product.onCreate();

        product.onUpdate();

        assertThat(product.getUpdatedAt()).isNotNull();
    }

}
