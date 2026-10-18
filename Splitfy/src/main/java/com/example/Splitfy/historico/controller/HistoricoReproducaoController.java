package com.example.Splitfy.historico.controller;

import com.example.Splitfy.historico.dto.HistoricoReproducaoResposta;
import com.example.Splitfy.historico.service.HistoricoReproducaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios/{usuarioId}/historico")
@Tag(name = "Histórico de reprodução")
public class HistoricoReproducaoController {

    private final HistoricoReproducaoService historicoService;

    public HistoricoReproducaoController(HistoricoReproducaoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or #p0.toString() == authentication.name")
    @Operation(summary = "Consultar histórico de reprodução",
            description = "Lista as reproduções do usuário, da mais recente para a mais antiga. O próprio usuário ou ADMIN pode consultar.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico consultado"),
            @ApiResponse(responseCode = "400", description = "Limite inválido"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public List<HistoricoReproducaoResposta> consultar(
            @Parameter(description = "ID do usuário") @PathVariable Long usuarioId,
            @Parameter(description = "Máximo de registros (1 a 100)") @RequestParam(defaultValue = "20") int limite
    ) {
        return historicoService.consultar(usuarioId, limite);
    }
}
