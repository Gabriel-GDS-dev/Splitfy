package com.example.Splitfy.seguranca;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentacaoOpenApi {
    @Bean
    GlobalOpenApiCustomizer documentarAutenticacao() {
        return api -> {
            api.getInfo().setDescription(api.getInfo().getDescription()
                    + " Para testar: execute POST /api/auth/login com email e senha, copie tokenAcesso e cole em Authorize (somente o token). "
                    + "Use IDs existentes; 0 é apenas exemplo. Consultas de usuários e alterações do catálogo exigem ADMIN.");
            api.getPaths().forEach((caminho, item) -> item.readOperationsMap().forEach((metodo, operacao) -> {
                boolean publica = metodo == io.swagger.v3.oas.models.PathItem.HttpMethod.POST
                        && (caminho.equals("/api/auth/login") || caminho.equals("/api/usuarios"));
                if (!publica) {
                    if (operacao.getSecurity() == null) {
                        operacao.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                    }
                    operacao.getResponses().putIfAbsent("401", new ApiResponse().description("Token ausente, inválido ou expirado. Faça login e use Authorize."));
                    operacao.getResponses().putIfAbsent("403", new ApiResponse().description("Conta sem permissão para esta operação."));
                }
            }));
        };
    }

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
                .pathsToMatch("/api/auth/login", "/api/auth/me", "/api/usuarios/*/historico", "/api/musicas/*/play")
                .build();
    }
}
