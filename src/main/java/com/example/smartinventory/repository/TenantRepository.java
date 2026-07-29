package com.example.smartinventory.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.Tenant;

/** Repository for {@link Tenant} persistence operations. */
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /**
     * Finds a tenant by its slug, the value stored on every tenant-owned row.
     *
     * @param slug the unique tenant slug
     * @return the matching tenant, or empty when no tenant carries that slug
     */
    Optional<Tenant> findBySlug(String slug);

    /**
     * Reports whether a tenant already uses the given slug.
     *
     * @param slug the slug to check
     * @return {@code true} when the slug is taken
     */
    boolean existsBySlug(String slug);

}
