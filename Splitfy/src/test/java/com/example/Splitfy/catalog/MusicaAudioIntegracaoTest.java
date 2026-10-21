package com.example.Splitfy.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MusicaAudioIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void audioPersisteSeparadoDoCatalogoComPermissoesESubstituicao() throws Exception {
        jdbc.update("INSERT INTO artista (id, nome) VALUES (901, 'Audio teste')");
        jdbc.update("INSERT INTO album (id, titulo, artista_id) VALUES (901, 'Audio teste', 901)");
        jdbc.update("INSERT INTO genero (id, nome) VALUES (901, 'Audio teste')");
        jdbc.update("INSERT INTO musica (id, titulo, duracao_segundos, album_id, genero_id) VALUES (901, 'Audio teste', 2, 901, 901)");
        String path = "/api/musicas/901/audio";
        byte[] mp3 = {(byte) 0xff, (byte) 0xfb, (byte) 0x90, 0, 1, 2, 3, 4};
        var arquivo = new MockMultipartFile("arquivo", "faixa.mp3", "audio/mpeg", mp3);
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var user = jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(user)).andExpect(status().isNotFound());
        mvc.perform(multipart(path).file(arquivo).with(r -> { r.setMethod("PUT"); return r; }).with(user))
                .andExpect(status().isForbidden());
        mvc.perform(multipart(path).file(new MockMultipartFile("arquivo", "fake.mp3", "audio/mpeg", "invalid".getBytes()))
                        .with(r -> { r.setMethod("PUT"); return r; }).with(admin))
                .andExpect(status().isBadRequest());
        for (int i = 0; i < 2; i++) {
            mvc.perform(multipart(path).file(arquivo).with(r -> { r.setMethod("PUT"); return r; }).with(admin))
                    .andExpect(status().isNoContent());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM musica_audio WHERE musica_id = 901", Integer.class)).isEqualTo(1);
        mvc.perform(get(path).with(user)).andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg")).andExpect(content().bytes(mp3));
        mvc.perform(get(path).header("Range", "bytes=0-3").with(user))
                .andExpect(status().isPartialContent()).andExpect(content().bytes(new byte[]{mp3[0], mp3[1], mp3[2], mp3[3]}));
        mvc.perform(get("/api/musicas/901").with(user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Audio teste"))
                .andExpect(jsonPath("$.audioDisponivel").value(true));
        jdbc.update("INSERT INTO musica (id, titulo, duracao_segundos, album_id, genero_id) VALUES (902, 'Sem MP3', 2, 901, 901)");
        mvc.perform(get("/api/musicas/902").with(user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.audioDisponivel").value(false));
        jdbc.update("DELETE FROM musica WHERE id = 901");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM musica_audio WHERE musica_id = 901", Integer.class)).isZero();
    }
}
