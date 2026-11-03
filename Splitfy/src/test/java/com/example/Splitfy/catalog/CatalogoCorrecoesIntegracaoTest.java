package com.example.Splitfy.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoCorrecoesIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @PersistenceContext EntityManager entityManager;

    @Test
    void adicionaDuasMusicasAoMesmoAlbumExistente() throws Exception {
        jdbc.update("INSERT INTO artista (id, nome) VALUES (953, 'Cantor existente')");
        jdbc.update("INSERT INTO album (id, titulo, artista_id) VALUES (953, 'Album existente', 953)");
        jdbc.update("INSERT INTO genero (id, nome) VALUES (953, 'Genero existente')");
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        for (int i = 1; i <= 2; i++) {
            mvc.perform(post("/api/musicas").with(admin).contentType("application/json")
                            .content("{\"titulo\":\"Faixa " + i + "\",\"duracaoSegundos\":200,\"albumId\":953,\"generoId\":953}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.albumId").value(953));
        }
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/albuns/953/musicas").with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM album WHERE artista_id = 953", Integer.class)).isEqualTo(1);
    }

    @Test
    void reutilizaAlbumDoMesmoArtistaMesmoComCaixaEEspacosDiferentes() throws Exception {
        jdbc.update("INSERT INTO artista (id, nome) VALUES (951, 'Cantor'), (952, 'Outro cantor')");
        jdbc.update("INSERT INTO album (id, titulo, artista_id) VALUES (951, ' Meu Album ', 951), (952, 'Meu Album', 952)");
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/albuns").with(admin).contentType("application/json")
                            .content("{\"titulo\":\"meu album\",\"artistaId\":951}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(951));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM album WHERE artista_id = 951", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM album WHERE artista_id = 952", Integer.class)).isEqualTo(1);
    }

    @Test
    void excluiMusicaComPlaylistAvaliacaoHistoricoEAudioSemExcluirOutrasMusicas() throws Exception {
        jdbc.update("INSERT INTO artista (id, nome) VALUES (951, 'Cantor')");
        jdbc.update("INSERT INTO album (id, titulo, artista_id) VALUES (951, 'Album', 951)");
        jdbc.update("INSERT INTO genero (id, nome) VALUES (951, 'Genero')");
        jdbc.update("INSERT INTO musica (id, titulo, duracao_segundos, album_id, genero_id) VALUES (951, 'Excluir', 100, 951, 951), (952, 'Manter', 100, 951, 951)");
        jdbc.update("INSERT INTO usuario (id, nome, email, senha_hash, data_nascimento) VALUES (951, 'Pessoa', 'correcoes@test.com', 'hash', '2000-01-01')");
        jdbc.update("INSERT INTO playlist (id, nome, usuario_id) VALUES (951, 'Playlist', 951)");
        for (long id : new long[]{951, 952}) {
            jdbc.update("INSERT INTO playlist_musica (playlist_id, musica_id) VALUES (951, ?)", id);
            jdbc.update("INSERT INTO avaliacao (usuario_id, musica_id) VALUES (951, ?)", id);
            jdbc.update("INSERT INTO historico_reproducao (usuario_id, musica_id) VALUES (951, ?)", id);
            jdbc.update("INSERT INTO musica_audio (musica_id, conteudo) VALUES (?, ?)", id, new byte[]{1, 2});
        }
        mvc.perform(delete("/api/musicas/951").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/musicas/951").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
        entityManager.flush();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM musica WHERE id = 951", Integer.class)).isZero();
        for (String table : new String[]{"playlist_musica", "avaliacao", "historico_reproducao", "musica_audio"}) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE musica_id = 951", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE musica_id = 952", Integer.class)).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM playlist WHERE id = 951", Integer.class)).isEqualTo(1);
    }
}
