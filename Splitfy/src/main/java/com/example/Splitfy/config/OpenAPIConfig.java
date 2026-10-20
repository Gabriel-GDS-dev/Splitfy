package com.example.Splitfy.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:swagger.properties")
public class OpenAPIConfig {
    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info().title("Splitfy - Gêneros e Músicas").version("v1")
                        .description("Documentação da parte de Gabriel: gêneros e músicas. "
                                + "Obtenha tokenAcesso em POST /api/auth/login com email e senha e cole "
                                + "somente o token em Authorize. Consultas exigem USER ou ADMIN; "
                                + "cadastro, edição e exclusão exigem ADMIN."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi generoMusicaApi() {
        return GroupedOpenApi.builder().group("generos-e-musicas")
                .pathsToMatch("/api/generos", "/api/generos/**", "/api/musicas", "/api/musicas/**")
                .build();
    }
}
