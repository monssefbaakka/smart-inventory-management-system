package com.example.smartinventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void onCreateSetsTimestamps() {
        Category category = new Category();

        category.onCreate();

        assertThat(category.getCreatedAt()).isNotNull();
        assertThat(category.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        Category category = new Category();
        category.onCreate();

        category.onUpdate();

        assertThat(category.getUpdatedAt()).isNotNull();
    }

}
