package com.example.Splitfy.playlist;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvaliacaoListaIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void listaSomenteAvaliacoesDoUsuarioComAutorizacao() throws Exception {
        jdbc.update("INSERT INTO usuario (id, nome, email, senha_hash, data_nascimento, role) VALUES (970, 'A', 'a970@test.local', 'hash', '2000-01-01', 'USER'), (971, 'B', 'b971@test.local', 'hash', '2000-01-01', 'USER')");
        jdbc.update("INSERT INTO artista (id, nome) VALUES (970, 'Artista')");
        jdbc.update("INSERT INTO album (id, titulo, artista_id) VALUES (970, 'Album', 970)");
        jdbc.update("INSERT INTO genero (id, nome) VALUES (970, 'Genero')");
        jdbc.update("INSERT INTO musica (id, titulo, duracao_segundos, album_id, genero_id) VALUES (970, 'Um', 2, 970, 970), (971, 'Dois', 2, 970, 970)");
        jdbc.update("INSERT INTO avaliacao (usuario_id, musica_id, curtido, nota) VALUES (970, 970, true, 5), (970, 971, false, 3), (971, 970, true, 4)");
        var user = jwt().jwt(token -> token.subject("970")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mvc.perform(get("/api/avaliacoes/usuarios/970")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/avaliacoes/usuarios/971").with(user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/avaliacoes/usuarios/970").with(user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].usuarioId").value(970))
                .andExpect(jsonPath("$[0].musicaId").value(970))
                .andExpect(jsonPath("$[0].curtido").value(true))
                .andExpect(jsonPath("$[1].nota").value(3));
        mvc.perform(get("/api/avaliacoes/usuarios/971").with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        jdbc.update("DELETE FROM avaliacao WHERE usuario_id = 971");
        mvc.perform(get("/api/avaliacoes/usuarios/971").with(admin)).andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mvc.perform(get("/api/avaliacoes/usuarios/999999").with(admin)).andExpect(status().isNotFound());
        mvc.perform(get("/api/avaliacoes/usuarios/0").with(admin)).andExpect(status().isBadRequest());
    }
}
