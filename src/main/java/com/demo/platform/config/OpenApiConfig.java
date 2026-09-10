package com.demo.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * Name of the HTTP Basic scheme every {@code /api/**} operation requires;
     * Swagger UI's "Authorize" button prompts for directory credentials.
     */
    public static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Computer Systems Management API")
                        .version("1.0.0")
                        .description("RESTful API for managing computer systems with comprehensive documentation, "
                                + "validation, pagination, and filtering capabilities. All /api/** endpoints require "
                                + "HTTP Basic credentials of an LDAP directory user; what a caller may do is decided "
                                + "by their Department:Role grants (see README, Role-Based Access Control).")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com")
                                .url("https://example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .components(new Components().addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("Directory username and password (e.g. admin / admin123)")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}
