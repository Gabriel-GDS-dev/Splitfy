package com.example.Splitfy.seguranca;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentacaoOpenApi {
    @Bean
    OpenAPI dadosApi() {
        return new OpenAPI()
                .info(new Info().title("Splitfy API").version("v1")
                        .description("Documentação das APIs do projeto Splitfy."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    GroupedOpenApi documentacaoSegurancaHistorico() {
        return GroupedOpenApi.builder()
                .group("seguranca-historico")
                .pathsToMatch("/api/auth/login", "/api/usuarios/*/historico", "/api/musicas/*/play")
                .build();
    }
}
