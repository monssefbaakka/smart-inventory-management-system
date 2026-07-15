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
import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    @Test
    void createSavesSupplier() {
        Supplier supplier = Supplier.builder().name("Acme").email("acme@example.com").build();
        when(supplierRepository.save(supplier)).thenReturn(supplier);

        Supplier result = supplierService.create(supplier);

        assertThat(result).isSameAs(supplier);
    }

    @Test
    void findByIdReturnsSupplierWhenPresent() {
        Supplier supplier = Supplier.builder().id(1L).build();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        assertThat(supplierService.findById(1L)).isSameAs(supplier);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllReturnsAllSuppliers() {
        Supplier supplier = Supplier.builder().id(1L).build();
        when(supplierRepository.findAll()).thenReturn(List.of(supplier));

        assertThat(supplierService.findAll()).containsExactly(supplier);
    }

    @Test
    void updateAppliesFieldsAndSaves() {
        Supplier existing = Supplier.builder().id(1L).name("Old").email("old@example.com").build();
        Supplier updated = Supplier.builder()
                .name("New")
                .contactName("Jane")
                .email("new@example.com")
                .phone("123")
                .address("addr")
                .build();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.update(1L, updated);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getContactName()).isEqualTo("Jane");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getPhone()).isEqualTo("123");
        assertThat(result.getAddress()).isEqualTo("addr");
    }

    @Test
    void deleteRemovesExistingSupplier() {
        Supplier existing = Supplier.builder().id(1L).build();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));

        supplierService.delete(1L);

        verify(supplierRepository).delete(existing);
    }

}
