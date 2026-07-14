package com.example.smartinventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/** OpenAPI/Swagger documentation configuration. */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the OpenAPI descriptor used to render the Swagger UI.
     *
     * @return the configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI smartInventoryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Inventory Management System API")
                        .description("REST API for managing products, categories and suppliers.")
                        .version("v0.1")
                        .license(new License().name("MIT")));
    }

}
