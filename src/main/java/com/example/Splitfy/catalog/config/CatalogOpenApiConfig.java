package com.example.Splitfy.catalog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Documentação Swagger do catálogo (artistas e álbuns): http://localhost:8080/swagger-ui.html
@Configuration
@OpenAPIDefinition(info = @Info(title = "Splitfy API", version = "v1",
        description = "Documentação das APIs do projeto Splitfy."))
@SecurityScheme(name = CatalogOpenApiConfig.BASIC_AUTH, type = SecuritySchemeType.HTTP, scheme = "basic")
public class CatalogOpenApiConfig {

    public static final String BASIC_AUTH = "basicAuth";

    @Bean
    GroupedOpenApi documentacaoCatalogo() {
        return GroupedOpenApi.builder()
                .group("catalogo")
                .displayName("Catálogo - Artistas e Álbuns")
                .pathsToMatch("/api/artistas/**", "/api/albuns/**")
                .build();
    }
}
