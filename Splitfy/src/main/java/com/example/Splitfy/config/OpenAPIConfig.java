package com.example.Splitfy.config;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:swagger.properties")
public class OpenAPIConfig {
    @Bean
    public GroupedOpenApi splitfyCompletoApi() {
        return GroupedOpenApi.builder().group("splitfy-completo")
                .pathsToMatch("/api/**").build();
    }

    @Bean
    public GroupedOpenApi gestaoSiteApi() {
        return GroupedOpenApi.builder().group("gestao-do-site")
                .pathsToMatch("/api/auth/login", "/api/usuarios", "/api/usuarios/*", "/api/usuarios/*/ativar",
                        "/api/usuarios/*/perfil", "/api/usuarios/*/playlists",
                        "/api/artistas", "/api/artistas/**", "/api/albuns", "/api/albuns/**",
                        "/api/playlists", "/api/playlists/**", "/api/avaliacoes", "/api/avaliacoes/**")
                .addOpenApiCustomizer(api -> {
                    api.info(new Info().title("Splitfy - Gestão do site").version("v1")
                            .description("Cadastros e edição do site. A gestão de usuários, papéis e catálogo exige ADMIN. "
                                    + "PUT /api/usuarios/{id} permite alterar role entre USER e ADMIN. "
                                    + "Cadastro público de conta cria USER."));
                    if (api.getPaths().get("/api/usuarios") != null
                            && api.getPaths().get("/api/usuarios").getPost() != null) {
                        api.getPaths().get("/api/usuarios").getPost().setSecurity(java.util.List.of(
                                new io.swagger.v3.oas.models.security.SecurityRequirement(),
                                new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth")));
                    }
                }).build();
    }

    @Bean
    public GroupedOpenApi generoMusicaApi() {
        return GroupedOpenApi.builder().group("generos-e-musicas")
                .pathsToMatch("/api/auth/login", "/api/generos", "/api/generos/**", "/api/musicas", "/api/musicas/**")
                .addOpenApiCustomizer(api -> api.info(new Info()
                        .title("Splitfy - Gêneros e Músicas").version("v1")
                        .description("Para obter o token, execute POST /api/auth/login neste grupo. "
                                + "Copie somente tokenAcesso e cole em Authorize, sem Bearer e sem URL. "
                                + "Consultas exigem USER ou ADMIN; cadastro, edição e exclusão exigem ADMIN.")))
                .build();
    }
}
