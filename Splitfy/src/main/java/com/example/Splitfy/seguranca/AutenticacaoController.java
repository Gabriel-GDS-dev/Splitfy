package com.example.Splitfy.seguranca;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.example.Splitfy.catalog.service.UsuarioService;
import com.example.Splitfy.catalog.dto.UsuarioResponse;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Emissão do JWT de acesso.")
public class AutenticacaoController {
    private final AutenticacaoService autenticacao;
    private final UsuarioService usuarios;

    public AutenticacaoController(AutenticacaoService autenticacao, UsuarioService usuarios) {
        this.autenticacao = autenticacao;
        this.usuarios = usuarios;
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar sessão atual", description = "Retorna os dados atuais da conta. Mudança de permissão ou desativação exige novo login.")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    public UsuarioResponse sessao(@AuthenticationPrincipal Jwt jwt) {
        UsuarioResponse usuario = usuarios.buscar(Long.valueOf(jwt.getSubject()));
        if (!usuario.ativo() || !usuario.role().equals(jwt.getClaimAsString("role"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sua permissão foi alterada. Entre novamente.");
        }
        return usuario;
    }

    @PostMapping("/login")
    @Operation(summary = "Entrar", description = "Autentica com e-mail e senha e retorna um token Bearer. Validade padrão: 3600 segundos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou usuário inativo")
    })
    public TokenResposta login(@RequestBody LoginRequisicao requisicao) {
        return autenticacao.entrar(requisicao);
    }
}
