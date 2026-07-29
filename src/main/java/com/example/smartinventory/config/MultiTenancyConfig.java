package com.example.smartinventory.config;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.smartinventory.tenant.TenantIdentifierResolver;

/** Wires the tenant resolver into Hibernate so {@code @TenantId} discrimination is applied. */
@Configuration
public class MultiTenancyConfig {

    /**
     * Registers the resolver Hibernate consults for the current tenant.
     *
     * @param tenantIdentifierResolver the resolver reading the per-request tenant context
     * @return a customizer adding the resolver to Hibernate's settings
     */
    @Bean
    public HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer(
            TenantIdentifierResolver tenantIdentifierResolver) {
        return (Map<String, Object> hibernateProperties) -> hibernateProperties
                .put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
    }

}
