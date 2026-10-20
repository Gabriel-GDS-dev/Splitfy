package com.example.Splitfy.catalog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Documentação Swagger do catálogo (artistas e álbuns): http://localhost:8080/swagger-ui.html
@Configuration
@OpenAPIDefinition(info = @Info(title = "Splitfy API", version = "v1",
        description = "Documentação das APIs do projeto Splitfy."))
public class CatalogOpenApiConfig {

    @Bean
    GroupedOpenApi documentacaoCatalogo() {
        return GroupedOpenApi.builder()
                .group("catalogo")
                .displayName("Catálogo - Artistas e Álbuns")
                .pathsToMatch("/api/artistas/**", "/api/albuns/**")
                .build();
    }
}
