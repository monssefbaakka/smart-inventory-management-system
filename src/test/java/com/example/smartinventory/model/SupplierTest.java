package com.example.smartinventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class SupplierTest {

    @Test
    void onCreateSetsTimestamps() {
        Supplier supplier = new Supplier();

        supplier.onCreate();

        assertThat(supplier.getCreatedAt()).isNotNull();
        assertThat(supplier.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        Supplier supplier = new Supplier();
        supplier.onCreate();

        supplier.onUpdate();

        assertThat(supplier.getUpdatedAt()).isNotNull();
    }

}
