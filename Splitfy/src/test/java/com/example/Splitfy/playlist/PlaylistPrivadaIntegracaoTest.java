package com.example.Splitfy.playlist;

import com.example.Splitfy.catalog.entity.Usuario;
import com.example.Splitfy.catalog.repository.UsuarioRepository;
import com.example.Splitfy.playlist.entity.Playlist;
import com.example.Splitfy.playlist.repository.PlaylistRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PlaylistPrivadaIntegracaoTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PlaylistRepository playlists;
    @Autowired PasswordEncoder senhas;

    @Test
    void playlistPrivadaSoApareceParaODonoEParaAdmin() throws Exception {
        Usuario dono = usuarios.save(new Usuario("Dono", "dono-pl@splitfy.local",
                senhas.encode("senhaDono123"), LocalDate.of(2000, 1, 1), "USER"));
        Usuario outro = usuarios.save(new Usuario("Outro", "outro-pl@splitfy.local",
                senhas.encode("senhaOutro123"), LocalDate.of(2000, 1, 1), "USER"));
        usuarios.save(new Usuario("Admin", "admin-pl@splitfy.local",
                senhas.encode("senhaAdmin123"), LocalDate.of(2000, 1, 1), "ADMIN"));

        Playlist privada = playlists.save(new Playlist("Privada do dono", null, false, dono.getId()));
        Playlist publica = playlists.save(new Playlist("Publica do dono", null, true, dono.getId()));

        String tokenDono = login("dono-pl@splitfy.local", "senhaDono123");
        String tokenOutro = login("outro-pl@splitfy.local", "senhaOutro123");
        String tokenAdmin = login("admin-pl@splitfy.local", "senhaAdmin123");

        // Listagem geral
        assertThat(ids(tokenDono, "/api/playlists")).contains(privada.getId(), publica.getId());
        assertThat(ids(tokenOutro, "/api/playlists")).contains(publica.getId()).doesNotContain(privada.getId());
        assertThat(ids(tokenAdmin, "/api/playlists")).contains(privada.getId(), publica.getId());

        // Listagem por usuario
        String porDono = "/api/usuarios/" + dono.getId() + "/playlists";
        assertThat(ids(tokenDono, porDono)).contains(privada.getId(), publica.getId());
        assertThat(ids(tokenOutro, porDono)).containsExactly(publica.getId());

        // Detalhe e musicas: privada vira 404 para terceiros
        mvc.perform(get("/api/playlists/{id}", privada.getId()).header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/playlists/{id}/musicas", privada.getId()).header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/playlists/{id}", privada.getId()).header("Authorization", "Bearer " + tokenDono))
                .andExpect(status().isOk());
        mvc.perform(get("/api/playlists/{id}", publica.getId()).header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isOk());

        // Nao da para editar/excluir a playlist publica de outro informando o id do dono no corpo
        mvc.perform(put("/api/playlists/{id}", publica.getId()).header("Authorization", "Bearer " + tokenOutro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Invadida\",\"publica\":true,\"usuarioId\":" + dono.getId() + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/playlists/{id}", publica.getId()).param("usuarioId", dono.getId().toString())
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isForbidden());

        // Nao da para criar playlist em nome de outro usuario
        mvc.perform(post("/api/playlists").header("Authorization", "Bearer " + tokenOutro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Falsa\",\"usuarioId\":" + dono.getId() + "}"))
                .andExpect(status().isForbidden());

        // Dono consegue tornar a playlist privada e ela some para os demais
        mvc.perform(put("/api/playlists/{id}", publica.getId()).header("Authorization", "Bearer " + tokenDono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Agora privada\",\"publica\":false,\"usuarioId\":" + dono.getId() + "}"))
                .andExpect(status().isOk());
        assertThat(ids(tokenOutro, "/api/playlists")).doesNotContain(publica.getId(), privada.getId());
    }

    private List<Long> ids(String token, String url) throws Exception {
        String corpo = mvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(corpo, "$[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    private String login(String email, String senha) throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.tokenAcesso");
    }
}
