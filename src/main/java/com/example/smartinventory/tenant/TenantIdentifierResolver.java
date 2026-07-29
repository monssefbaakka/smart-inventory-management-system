package com.example.smartinventory.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tells Hibernate which tenant every statement belongs to. Reads {@link TenantContext} first and
 * falls back to the configured default tenant, which covers work that runs outside a request.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    private final String defaultTenant;

    public TenantIdentifierResolver(@Value("${multitenancy.default-tenant}") String defaultTenant) {
        this.defaultTenant = defaultTenant;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : defaultTenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

}
