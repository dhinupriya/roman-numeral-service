package com.adobe.romannumeral.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 configuration — defines API metadata visible in Swagger UI.
 *
 * <p>Accessible at:
 * <ul>
 *   <li>{@code /swagger-ui.html} — interactive Swagger UI</li>
 *   <li>{@code /v3/api-docs} — OpenAPI 3.0 JSON spec (for client code generators)</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI romanNumeralOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Roman Numeral Conversion API")
                        .description("Production-grade HTTP service that converts integers (1-3999) "
                                + "to Roman numerals. Supports single conversions and parallel range "
                                + "queries with chunked virtual thread execution. "
                                + "Built with Java 21, Spring Boot 3.4, and Clean Architecture.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Adobe Engineering Assessment"))
                        .license(new License()
                                .name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList("API Key"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("API Key", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API key for authentication. "
                                        + "Provide via the X-API-Key header.")));
    }
}
