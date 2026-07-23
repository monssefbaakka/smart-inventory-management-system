package com.example.smartinventory.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

class OpenApiConfigTest {

    private final OpenAPI openApi = new OpenApiConfig().smartInventoryOpenApi();

    @Test
    void exposesApiInfo() {
        assertThat(openApi.getInfo()).isNotNull();
        assertThat(openApi.getInfo().getTitle()).isEqualTo("Smart Inventory Management System API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1.0");
        assertThat(openApi.getInfo().getLicense().getName()).isEqualTo("MIT");
    }

    @Test
    void declaresJwtBearerSecurityScheme() {
        SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get(OpenApiConfig.SECURITY_SCHEME_NAME);

        assertThat(scheme).isNotNull();
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    void appliesBearerAuthGlobally() {
        assertThat(openApi.getSecurity())
                .anySatisfy(requirement -> assertThat(requirement).containsKey(OpenApiConfig.SECURITY_SCHEME_NAME));
    }

    @Test
    void declaresAtLeastOneServer() {
        assertThat(openApi.getServers()).isNotEmpty();
    }

}
