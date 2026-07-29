package com.example.smartinventory.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantIdentifierResolverTest {

    private final TenantIdentifierResolver resolver = new TenantIdentifierResolver("default");

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resolvesTenantBoundToCurrentThread() {
        TenantContext.setTenantId("acme");

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("acme");
    }

    @Test
    void fallsBackToDefaultTenantOutsideRequests() {
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("default");
    }

    @Test
    void doesNotValidateExistingSessions() {
        assertThat(resolver.validateExistingCurrentSessions()).isFalse();
    }

}
