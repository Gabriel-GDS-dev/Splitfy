package com.example.Splitfy.historico.controller;

import com.example.Splitfy.historico.dto.HistoricoReproducaoResposta;
import com.example.Splitfy.historico.service.HistoricoReproducaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios/{usuarioId}/historico")
public class HistoricoReproducaoController {

    private final HistoricoReproducaoService historicoService;

    public HistoricoReproducaoController(HistoricoReproducaoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping
    public List<HistoricoReproducaoResposta> consultar(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "20") int limite
    ) {
        return historicoService.consultar(usuarioId, limite);
    }
}
