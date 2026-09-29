package com.example.Splitfy.playlist.controller;

import com.example.Splitfy.playlist.dto.AvaliacaoRequest;
import com.example.Splitfy.playlist.dto.AvaliacaoResponse;
import com.example.Splitfy.playlist.service.AvaliacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @PostMapping
    public ResponseEntity<AvaliacaoResponse> criar(@RequestBody AvaliacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(avaliacaoService.criar(request));
    }

    @GetMapping("/{usuarioId}/{musicaId}")
    public AvaliacaoResponse buscar(@PathVariable Long usuarioId, @PathVariable Long musicaId) {
        return avaliacaoService.buscar(usuarioId, musicaId);
    }

    @PutMapping("/{usuarioId}/{musicaId}")
    public AvaliacaoResponse atualizar(
            @PathVariable Long usuarioId,
            @PathVariable Long musicaId,
            @RequestBody AvaliacaoRequest request) {
        return avaliacaoService.atualizar(usuarioId, musicaId, request);
    }

    @DeleteMapping("/{usuarioId}/{musicaId}")
    public ResponseEntity<Void> excluir(@PathVariable Long usuarioId, @PathVariable Long musicaId) {
        avaliacaoService.excluir(usuarioId, musicaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/musicas/{musicaId}/curtidas")
    public Map<String, Long> contarCurtidas(@PathVariable Long musicaId) {
        return Map.of("total", avaliacaoService.contarCurtidas(musicaId));
    }
}
