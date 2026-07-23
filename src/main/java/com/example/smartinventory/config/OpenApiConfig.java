package com.example.smartinventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI/Swagger documentation configuration.
 *
 * <p>Registers the API descriptor rendered by the Swagger UI and declares a JWT bearer security
 * scheme so the interactive docs expose an <em>Authorize</em> button. Once a token obtained from
 * {@code POST /api/auth/login} is supplied there, it is sent as an {@code Authorization: Bearer}
 * header on every request the UI issues.
 */
@Configuration
public class OpenApiConfig {

    /** Name of the JWT bearer security scheme referenced by protected operations. */
    static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Builds the OpenAPI descriptor used to render the Swagger UI.
     *
     * @return the configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI smartInventoryOpenApi() {
        return new OpenAPI()
                .info(apiInfo())
                .addServersItem(new Server()
                        .url("/")
                        .description("Current host"))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    private Info apiInfo() {
        return new Info()
                .title("Smart Inventory Management System API")
                .description("""
                        REST API for managing products, categories, suppliers, stock movements, \
                        purchase orders, inventory reports and the dashboard.

                        Authentication is stateless and JWT based: register or log in through the \
                        `Auth` endpoints, copy the returned token, click **Authorize** and paste it \
                        to call the protected endpoints. Write operations generally require the \
                        `ADMIN` role.""")
                .version("v1.0")
                .contact(new Contact()
                        .name("Smart Inventory Management System")
                        .url("https://github.com/monssefbaakka/smart-inventory-management-system"))
                .license(new License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste a JWT obtained from POST /api/auth/login (without the 'Bearer ' prefix).");
    }

}
