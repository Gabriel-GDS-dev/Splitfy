package com.example.Splitfy.seguranca;

import com.example.Splitfy.catalog.entity.Usuario;
import com.example.Splitfy.catalog.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutenticacaoService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final JwtEncoder tokens;
    private final long expiracaoSegundos;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder senhas, JwtEncoder tokens,
                              @Value("${splitfy.jwt.expiracao-segundos:3600}") long expiracaoSegundos) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
        this.expiracaoSegundos = expiracaoSegundos;
    }

    @Transactional
    public TokenResposta entrar(LoginRequisicao requisicao) {
        if (requisicao == null || requisicao.email() == null || requisicao.senha() == null) {
            throw credenciaisInvalidas();
        }
        Usuario usuario = usuarios.findByEmailIgnoreCase(requisicao.email().trim())
                .orElseThrow(this::credenciaisInvalidas);
        if (!usuario.isAtivo()) {
            throw credenciaisInvalidas();
        }

        String hash = usuario.getSenhaHash();
        if (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
            if (!senhas.matches(requisicao.senha(), hash)) {
                throw credenciaisInvalidas();
            }
        } else {
            // Senhas gravadas pelo CRUD anterior passam a BCrypt no primeiro login correto.
            if (!MessageDigest.isEqual(requisicao.senha().getBytes(StandardCharsets.UTF_8),
                    hash.getBytes(StandardCharsets.UTF_8))) {
                throw credenciaisInvalidas();
            }
            usuario.setSenhaHash(senhas.encode(requisicao.senha()));
            usuarios.save(usuario);
        }

        Instant agora = Instant.now();
        JwtClaimsSet dados = JwtClaimsSet.builder()
                .issuer("splitfy")
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(expiracaoSegundos))
                .claim("role", usuario.getRole())
                .build();
        String tokenAcesso = tokens.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), dados)).getTokenValue();
        return new TokenResposta(tokenAcesso, "Bearer", expiracaoSegundos);
    }

    private ResponseStatusException credenciaisInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou senha invalidos.");
    }
}
