package com.example.Splitfy.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@SpringBootTest
@AutoConfigureMockMvc
class SwaggerIntegracaoTest {
    @Autowired MockMvc mvc;

    @Test
    void todosOsGruposIncluemLoginETodasAsApisEstaoIntegradas() throws Exception {
        for (String grupo : new String[]{"gestao-do-site", "generos-e-musicas", "seguranca-historico", "splitfy-completo"}) {
            String json = mvc.perform(get("/v3/api-docs/" + grupo)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode doc = JsonMapper.builder().build().readTree(json);
            assertThat(doc.path("paths").has("/api/auth/login")).isTrue();
            assertThat(doc.path("paths").path("/api/auth/login").path("post").path("security").size()).isZero();
            if (grupo.equals("splitfy-completo")) {
                assertThat(doc.path("paths").propertyNames()).contains(
                        "/api/generos", "/api/musicas", "/api/usuarios", "/api/playlists", "/api/albuns", "/api/artistas");
            }
            if (grupo.equals("gestao-do-site")) {
                JsonNode consulta = doc.path("paths").path("/api/usuarios/{id}").path("get");
                assertThat(consulta.at("/security/0/bearerAuth").isArray()).isTrue();
                assertThat(consulta.path("responses").has("401")).isTrue();
                assertThat(consulta.path("responses").has("403")).isTrue();
            }
        }
    }

    @Test
    void documentacaoPublicaDescreveSomenteGenerosEMusicas() throws Exception {
        for (String endpoint : new String[]{"/v3/api-docs/generos-e-musicas"}) {
            String json = mvc.perform(get(endpoint)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode doc = JsonMapper.builder().build().readTree(json);
            assertThat(doc.path("paths").propertyNames()).containsExactlyInAnyOrder(
                    "/api/generos", "/api/generos/{id}", "/api/generos/{id}/musicas",
                    "/api/musicas", "/api/musicas/{id}", "/api/musicas/{id}/audio", "/api/musicas/{id}/play", "/api/auth/login");
            assertThat(doc.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
            JsonNode paths = doc.path("paths");
            int operacoes = 0;
            for (JsonNode path : paths) {
                for (JsonNode operation : path) {
                    assertThat(operation.path("summary").asText()).isNotBlank();
                    if (operation.path("operationId").asText().equals("login")) {
                        assertThat(operation.path("security").size()).isZero();
                        continue;
                    }
                    assertThat(operation.at("/security/0/bearerAuth").isArray()).isTrue();
                    assertThat(operation.path("responses").has("401")).isTrue();
                    assertThat(operation.path("responses").has("403")).isTrue();
                    operacoes++;
                }
            }
            assertThat(operacoes).isEqualTo(14);
            JsonNode criar = paths.path("/api/musicas").path("post");
            assertThat(criar.at("/requestBody/required").asBoolean()).isTrue();
            assertThat(criar.at("/requestBody/content/application~1json/schema/$ref").asText())
                    .endsWith("/MusicaRequest");
            assertThat(criar.path("responses").has("201")).isTrue();
            assertThat(paths.path("/api/generos/{id}").path("delete").path("responses").has("204")).isTrue();
            assertThat(paths.path("/api/musicas").path("get").path("parameters").size()).isEqualTo(3);
        }
    }

    @Test
    void loginDisponivelNoGrupoDeSegurancaSemExigirToken() throws Exception {
        String json = mvc.perform(get("/v3/api-docs/seguranca-historico"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode doc = JsonMapper.builder().build().readTree(json);
        JsonNode login = doc.path("paths").path("/api/auth/login").path("post");
        assertThat(login.path("summary").asText()).isEqualTo("Entrar");
        assertThat(login.path("security").size()).isZero();
        assertThat(login.path("responses").has("200")).isTrue();
        assertThat(login.path("responses").has("401")).isTrue();
        mvc.perform(get("/api/generos").header("Authorization",
                "Bearer http://localhost:8080/.well-known/oauth-protected-resource"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerAbreSemTokenEApiContinuaProtegida() throws Exception {
        mvc.perform(get("/manifest.webmanifest")).andExpect(status().isOk());
        mvc.perform(get("/sw.js")).andExpect(status().isOk());
        mvc.perform(get("/assets/icons/icon-192.png")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
        mvc.perform(get("/api/generos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/musicas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/generos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/generos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/musicas").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }
}
