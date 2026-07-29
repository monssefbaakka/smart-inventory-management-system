package com.example.smartinventory.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void tenantIdIsUnsetByDefault() {
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void setTenantIdBindsValueToCurrentThread() {
        TenantContext.setTenantId("acme");

        assertThat(TenantContext.getTenantId()).isEqualTo("acme");
    }

    @Test
    void clearUnbindsValue() {
        TenantContext.setTenantId("acme");

        TenantContext.clear();

        assertThat(TenantContext.getTenantId()).isNull();
    }

}
