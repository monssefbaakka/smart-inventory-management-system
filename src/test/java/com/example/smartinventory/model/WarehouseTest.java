package com.example.smartinventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class WarehouseTest {

    @Test
    void onCreateSetsTimestamps() {
        Warehouse warehouse = new Warehouse();

        warehouse.onCreate();

        assertThat(warehouse.getCreatedAt()).isNotNull();
        assertThat(warehouse.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        Warehouse warehouse = new Warehouse();
        warehouse.onCreate();

        warehouse.onUpdate();

        assertThat(warehouse.getUpdatedAt()).isNotNull();
    }

    @Test
    void builderDefaultsToActive() {
        assertThat(Warehouse.builder().code("WH-1").name("Main").build().getActive()).isTrue();
    }

    @Test
    void onCreateDefaultsUnsetActiveFlagToTrue() {
        Warehouse warehouse = new Warehouse();

        warehouse.onCreate();

        assertThat(warehouse.getActive()).isTrue();
    }

}
