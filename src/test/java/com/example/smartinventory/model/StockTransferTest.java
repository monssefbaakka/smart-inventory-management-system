package com.example.smartinventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class StockTransferTest {

    @Test
    void onCreateSetsCreatedAt() {
        StockTransfer transfer = new StockTransfer();

        transfer.onCreate();

        assertThat(transfer.getCreatedAt()).isNotNull();
    }

    @Test
    void builderKeepsBothSidesOfTheMove() {
        StockTransfer transfer = StockTransfer.builder()
                .sourceWarehouse(Warehouse.builder().id(1L).code("WH-NORTH").build())
                .destinationWarehouse(Warehouse.builder().id(2L).code("WH-SOUTH").build())
                .quantity(6)
                .build();

        assertThat(transfer.getSourceWarehouse().getCode()).isEqualTo("WH-NORTH");
        assertThat(transfer.getDestinationWarehouse().getCode()).isEqualTo("WH-SOUTH");
        assertThat(transfer.getQuantity()).isEqualTo(6);
    }

}
