package com.example.Splitfy.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FontesMusicaisIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void cadastroPersisteFontesValidaHostsEImagensMantendoAcessoRestrito() throws Exception {
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var user = jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
        jdbc.update("INSERT INTO artista (id, nome) VALUES (951, 'Fontes teste')");
        jdbc.update("INSERT INTO album (id, titulo, artista_id) VALUES (951, 'Fontes teste', 951)");
        jdbc.update("INSERT INTO genero (id, nome) VALUES (951, 'Fontes teste')");
        String body = """
                {"titulo":"Faixa","duracaoSegundos":200,"albumId":951,"generoId":951,
                 "spotifyUrl":"https://open.spotify.com/track/1234567890123456789012?si=abc",
                 "youtubeUrl":"https://music.youtube.com/watch?v=abcdefghijk&list=abc","fontePreferida":"AUTO"}
                """;
        String resposta = mvc.perform(post("/api/musicas").with(admin).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.youtubeUrl").value("https://www.youtube.com/watch?v=abcdefghijk"))
                .andExpect(jsonPath("$.spotifyUrl").value("https://open.spotify.com/track/1234567890123456789012"))
                .andReturn().getResponse().getContentAsString();
        long id = com.jayway.jsonpath.JsonPath.<Number>read(resposta, "$.id").longValue();
        mvc.perform(get("/api/musicas/{id}", id).with(user)).andExpect(status().isOk()).andExpect(jsonPath("$.fontePreferida").value("AUTO"));
        mvc.perform(put("/api/musicas/{id}", id).with(admin).contentType("application/json")
                        .content(body.replace("music.youtube.com", "music.youtube.com.evil.test"))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/albuns/951").with(admin).contentType("application/json")
                        .content("{\"titulo\":\"Album\",\"artistaId\":951,\"capaUrl\":\"https://images.example.test/capa.jpg\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.capaUrl").value("https://images.example.test/capa.jpg"));
        mvc.perform(put("/api/albuns/951").with(admin).contentType("application/json")
                        .content("{\"titulo\":\"Album\",\"artistaId\":951,\"capaUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/integracoes/musicas/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/integracoes/musicas/status").with(user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/integracoes/musicas/status").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/api/integracoes/musicas/buscar?q=a").with(admin)).andExpect(status().isBadRequest());
    }
}
