package com.example.smartinventory.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.exception.DuplicateTenantSlugException;
import com.example.smartinventory.exception.InactiveTenantException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Tenant;
import com.example.smartinventory.repository.TenantRepository;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void createSavesTenant() {
        Tenant tenant = Tenant.builder().slug("acme").name("Acme").build();
        when(tenantRepository.existsBySlug("acme")).thenReturn(false);
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        assertThat(tenantService.create(tenant)).isSameAs(tenant);
    }

    @Test
    void createThrowsWhenSlugAlreadyUsed() {
        Tenant tenant = Tenant.builder().slug("acme").name("Acme").build();
        when(tenantRepository.existsBySlug("acme")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.create(tenant))
                .isInstanceOf(DuplicateTenantSlugException.class);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void findByIdReturnsTenant() {
        Tenant tenant = Tenant.builder().id(1L).slug("acme").name("Acme").build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        assertThat(tenantService.findById(1L)).isSameAs(tenant);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findBySlugReturnsTenant() {
        Tenant tenant = Tenant.builder().id(1L).slug("acme").name("Acme").build();
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(tenant));

        assertThat(tenantService.findBySlug("acme")).isSameAs(tenant);
    }

    @Test
    void findBySlugThrowsWhenMissing() {
        when(tenantRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.findBySlug("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllReturnsTenants() {
        when(tenantRepository.findAll()).thenReturn(List.of(Tenant.builder().slug("acme").build()));

        assertThat(tenantService.findAll()).hasSize(1);
    }

    @Test
    void findActiveBySlugReturnsActiveTenant() {
        Tenant tenant = Tenant.builder().slug("acme").name("Acme").active(true).build();
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(tenant));

        assertThat(tenantService.findActiveBySlug("acme")).isSameAs(tenant);
    }

    @Test
    void findActiveBySlugThrowsWhenTenantIsInactive() {
        Tenant tenant = Tenant.builder().slug("acme").name("Acme").active(false).build();
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> tenantService.findActiveBySlug("acme"))
                .isInstanceOf(InactiveTenantException.class);
    }

    @Test
    void updateChangesNameAndActiveFlagButNotSlug() {
        Tenant existing = Tenant.builder().id(1L).slug("acme").name("Acme").active(true).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(existing)).thenReturn(existing);

        Tenant result = tenantService.update(1L,
                Tenant.builder().slug("other").name("Acme Corp").active(false).build());

        assertThat(result.getSlug()).isEqualTo("acme");
        assertThat(result.getName()).isEqualTo("Acme Corp");
        assertThat(result.getActive()).isFalse();
    }

    @Test
    void updateKeepsActiveFlagWhenPayloadOmitsIt() {
        Tenant existing = Tenant.builder().id(1L).slug("acme").name("Acme").active(true).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(existing)).thenReturn(existing);

        Tenant result = tenantService.update(1L, Tenant.builder().name("Acme Corp").active(null).build());

        assertThat(result.getActive()).isTrue();
    }

}
