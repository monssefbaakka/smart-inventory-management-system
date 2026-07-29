package com.example.smartinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.exception.DuplicateTenantSlugException;
import com.example.smartinventory.exception.InactiveTenantException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Tenant;
import com.example.smartinventory.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service managing the {@link Tenant} registry.
 *
 * <p>Tenants are the one resource that is not itself tenant-scoped: the registry is shared by the
 * whole installation and is therefore only reachable by administrators.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;

    /**
     * Registers a new tenant.
     *
     * @param tenant the tenant to create
     * @return the persisted tenant
     * @throws DuplicateTenantSlugException if another tenant already uses the slug
     */
    public Tenant create(Tenant tenant) {
        if (tenantRepository.existsBySlug(tenant.getSlug())) {
            throw new DuplicateTenantSlugException("Tenant slug already in use: " + tenant.getSlug());
        }
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant findById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + id));
    }

    /**
     * Finds a tenant by the slug stored on its rows.
     *
     * @param slug the unique tenant slug
     * @return the matching tenant
     * @throws ResourceNotFoundException if no tenant carries that slug
     */
    @Transactional(readOnly = true)
    public Tenant findBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with slug: " + slug));
    }

    @Transactional(readOnly = true)
    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    /**
     * Returns a tenant that may still be used, rejecting deactivated ones.
     *
     * @param slug the unique tenant slug
     * @return the matching, active tenant
     * @throws ResourceNotFoundException if no tenant carries that slug
     * @throws InactiveTenantException   if the tenant has been deactivated
     */
    @Transactional(readOnly = true)
    public Tenant findActiveBySlug(String slug) {
        Tenant tenant = findBySlug(slug);
        if (!Boolean.TRUE.equals(tenant.getActive())) {
            throw new InactiveTenantException("Tenant is not active: " + slug);
        }
        return tenant;
    }

    /**
     * Updates the mutable fields of an existing tenant. The slug is immutable, since it is stamped
     * on every row the tenant owns.
     *
     * @param id            identifier of the tenant to update
     * @param updatedTenant tenant carrying the new field values
     * @return the persisted, updated tenant
     */
    public Tenant update(Long id, Tenant updatedTenant) {
        Tenant existing = findById(id);
        existing.setName(updatedTenant.getName());
        if (updatedTenant.getActive() != null) {
            existing.setActive(updatedTenant.getActive());
        }
        return tenantRepository.save(existing);
    }

}
