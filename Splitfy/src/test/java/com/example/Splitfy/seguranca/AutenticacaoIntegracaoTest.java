package com.example.Splitfy.seguranca;

import com.jayway.jsonpath.JsonPath;
import com.example.Splitfy.catalog.entity.Usuario;
import com.example.Splitfy.catalog.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AutenticacaoIntegracaoTest {
    private static final Pattern TOKEN = Pattern.compile("\"tokenAcesso\":\"([^\"]+)\"");

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder senhas;

    @Test
    void loginJwtRolesEPropriedadeDoHistorico() throws Exception {
        Usuario usuario = usuarios.save(new Usuario("Usuario", "usuario-teste@splitfy.local", "senhaAntiga123",
                LocalDate.of(2000, 1, 1), "USER"));
        Usuario outro = usuarios.save(new Usuario("Outro", "outro-teste@splitfy.local",
                senhas.encode("senhaOutro123"), LocalDate.of(2000, 1, 1), "USER"));
        usuarios.save(new Usuario("Admin", "admin-teste@splitfy.local",
                senhas.encode("senhaAdmin123"), LocalDate.of(2000, 1, 1), "ADMIN"));

        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"usuario-teste@splitfy.local\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());

        String tokenUsuario = login("usuario-teste@splitfy.local", "senhaAntiga123");
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getSenhaHash()).startsWith("$2");
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/{id}/historico", usuario.getId())
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
        mvc.perform(get("/api/usuarios/{id}/historico", outro.getId())
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/musicas/1/play").header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + outro.getId() + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/musicas/999999/play").header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuario.getId() + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/usuarios/{id}/historico", usuario.getId())
                        .header("Authorization", "Bearer " + tokenUsuario + "alterado"))
                .andExpect(status().isUnauthorized());

        String tokenAdmin = login("admin-teste@splitfy.local", "senhaAdmin123");
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/usuarios/{id}/historico", usuario.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void cadastroPublicoFicaComoUserESenhaUsaBCrypt() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Novo\",\"email\":\"novo-teste@splitfy.local\","
                                + "\"senha\":\"senhaNova123\",\"dataNascimento\":\"2000-01-01\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Novo\",\"email\":\"novo-teste@splitfy.local\","
                                + "\"senha\":\"senhaNova123\",\"dataNascimento\":\"2000-01-01\"}"))
                .andExpect(status().isCreated());
        Usuario novo = usuarios.findByEmailIgnoreCase("novo-teste@splitfy.local").orElseThrow();
        assertThat(novo.getRole()).isEqualTo("USER");
        assertThat(senhas.matches("senhaNova123", novo.getSenhaHash())).isTrue();
    }

    @Test
    void swaggerDocumentaSomenteAsOperacoesDeSegurancaEHistorico() throws Exception {
        String configuracao = mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String caminhoGrupo = JsonPath.read(configuracao, "$.urls[0].url");
        String especificacao = mvc.perform(get(caminhoGrupo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> caminhos = JsonPath.read(especificacao, "$.paths");
        assertThat(caminhos.keySet()).containsExactlyInAnyOrder(
                "/api/auth/login", "/api/usuarios/{usuarioId}/historico", "/api/musicas/{id}/play");
        Map<String, Object> login = JsonPath.read(especificacao, "$.paths['/api/auth/login'].post");
        assertThat(login).doesNotContainKey("security");
        List<Map<String, Object>> segurancaHistorico = JsonPath.read(especificacao,
                "$.paths['/api/usuarios/{usuarioId}/historico'].get.security");
        List<Map<String, Object>> segurancaPlay = JsonPath.read(especificacao,
                "$.paths['/api/musicas/{id}/play'].post.security");
        assertThat(segurancaHistorico.getFirst()).containsKey("bearerAuth");
        assertThat(segurancaPlay.getFirst()).containsKey("bearerAuth");
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    private String login(String email, String senha) throws Exception {
        String resposta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = TOKEN.matcher(resposta);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
