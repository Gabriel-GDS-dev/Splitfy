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
    void documentacaoPublicaDescreveSomenteGenerosEMusicas() throws Exception {
        for (String endpoint : new String[]{"/v3/api-docs", "/v3/api-docs/generos-e-musicas"}) {
            String json = mvc.perform(get(endpoint)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode doc = JsonMapper.builder().build().readTree(json);
            assertThat(doc.path("paths").propertyNames()).containsExactlyInAnyOrder(
                    "/api/generos", "/api/generos/{id}", "/api/generos/{id}/musicas",
                    "/api/musicas", "/api/musicas/{id}", "/api/musicas/{id}/play");
            assertThat(doc.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
            JsonNode paths = doc.path("paths");
            int operacoes = 0;
            for (JsonNode path : paths) {
                for (JsonNode operation : path) {
                    assertThat(operation.path("summary").asText()).isNotBlank();
                    assertThat(operation.at("/security/0/bearerAuth").isArray()).isTrue();
                    assertThat(operation.path("responses").has("401")).isTrue();
                    assertThat(operation.path("responses").has("403")).isTrue();
                    operacoes++;
                }
            }
            assertThat(operacoes).isEqualTo(12);
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
    void swaggerAbreSemTokenEApiContinuaProtegida() throws Exception {
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
