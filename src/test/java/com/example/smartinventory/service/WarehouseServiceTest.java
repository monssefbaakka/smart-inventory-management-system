package com.example.smartinventory.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.WarehouseRepository;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    @Test
    void createSavesWarehouse() {
        Warehouse warehouse = Warehouse.builder().code("WH-1").name("Main Depot").build();
        when(warehouseRepository.save(warehouse)).thenReturn(warehouse);

        assertThat(warehouseService.create(warehouse)).isSameAs(warehouse);
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void findByIdReturnsWarehouseWhenPresent() {
        Warehouse warehouse = Warehouse.builder().id(1L).code("WH-1").build();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        assertThat(warehouseService.findById(1L)).isSameAs(warehouse);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void findByCodeReturnsWarehouseWhenPresent() {
        Warehouse warehouse = Warehouse.builder().id(1L).code("WH-1").build();
        when(warehouseRepository.findByCode("WH-1")).thenReturn(Optional.of(warehouse));

        assertThat(warehouseService.findByCode("WH-1")).isSameAs(warehouse);
    }

    @Test
    void findByCodeThrowsWhenMissing() {
        when(warehouseRepository.findByCode("WH-X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.findByCode("WH-X"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("WH-X");
    }

    @Test
    void findAllReturnsAllWarehouses() {
        Warehouse warehouse = Warehouse.builder().id(1L).build();
        when(warehouseRepository.findAll()).thenReturn(List.of(warehouse));

        assertThat(warehouseService.findAll()).containsExactly(warehouse);
    }

    @Test
    void updateAppliesFieldsAndSaves() {
        Warehouse existing = Warehouse.builder().id(1L).code("WH-1").name("Old").active(true).build();
        Warehouse updated = Warehouse.builder().code("WH-2").name("New").location("Rotterdam").active(false).build();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        Warehouse result = warehouseService.update(1L, updated);

        assertThat(result.getCode()).isEqualTo("WH-2");
        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getLocation()).isEqualTo("Rotterdam");
        assertThat(result.getActive()).isFalse();
    }

    @Test
    void updateKeepsActiveFlagWhenPayloadOmitsIt() {
        Warehouse existing = Warehouse.builder().id(1L).code("WH-1").name("Old").active(false).build();
        Warehouse updated = Warehouse.builder().code("WH-1").name("New").active(null).build();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(warehouseService.update(1L, updated).getActive()).isFalse();
    }

    @Test
    void deleteRemovesExistingWarehouse() {
        Warehouse existing = Warehouse.builder().id(1L).build();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));

        warehouseService.delete(1L);

        verify(warehouseRepository).delete(existing);
    }

}
