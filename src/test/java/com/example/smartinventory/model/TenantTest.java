package com.example.smartinventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TenantTest {

    @Test
    void onCreateSetsTimestamps() {
        Tenant tenant = new Tenant();

        tenant.onCreate();

        assertThat(tenant.getCreatedAt()).isNotNull();
        assertThat(tenant.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        Tenant tenant = new Tenant();
        tenant.onCreate();

        tenant.onUpdate();

        assertThat(tenant.getUpdatedAt()).isNotNull();
    }

    @Test
    void builderDefaultsToActive() {
        assertThat(Tenant.builder().slug("acme").name("Acme").build().getActive()).isTrue();
    }

    @Test
    void onCreateDefaultsUnsetActiveFlagToTrue() {
        Tenant tenant = new Tenant();

        tenant.onCreate();

        assertThat(tenant.getActive()).isTrue();
    }

}
