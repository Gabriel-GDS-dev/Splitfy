package com.example.Splitfy.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SwaggerArtistaAlbumIntegracaoTest {
    @Autowired MockMvc mvc;

    @Test
    void documentacaoDescreveTodasAsOperacoesDeArtistasEAlbuns() throws Exception {
        String json = mvc.perform(get("/v3/api-docs/gestao-do-site")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode paths = JsonMapper.builder().build().readTree(json).path("paths");
        int operacoes = 0;
        for (String rota : new String[]{"/api/artistas", "/api/artistas/{id}", "/api/artistas/{id}/albuns",
                "/api/albuns", "/api/albuns/{id}", "/api/albuns/{id}/musicas"}) {
            assertThat(paths.has(rota)).as(rota).isTrue();
            for (JsonNode operation : paths.path(rota)) {
                assertThat(operation.path("summary").asText()).isNotBlank();
                assertThat(operation.at("/security/0/bearerAuth").isArray()).isTrue();
                assertThat(operation.path("responses").has("401")).isTrue();
                assertThat(operation.path("responses").has("403")).isTrue();
                operacoes++;
            }
        }
        assertThat(operacoes).isEqualTo(12);
        JsonNode criarAlbum = paths.path("/api/albuns").path("post");
        assertThat(criarAlbum.at("/requestBody/required").asBoolean()).isTrue();
        assertThat(criarAlbum.at("/requestBody/content/application~1json/schema/$ref").asText()).endsWith("/AlbumRequest");
        assertThat(criarAlbum.path("responses").has("201")).isTrue();
        assertThat(paths.path("/api/artistas/{id}").path("delete").path("responses").has("409")).isTrue();
        JsonNode parametrosListarAlbuns = paths.path("/api/albuns").path("get").path("parameters");
        assertThat(parametrosListarAlbuns.findValuesAsString("name"))
                .containsExactlyInAnyOrder("ano", "artistaId", "page", "size", "sort");
    }
}
