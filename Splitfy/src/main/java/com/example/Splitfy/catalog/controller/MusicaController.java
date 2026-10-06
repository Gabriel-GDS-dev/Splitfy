package com.example.Splitfy.catalog.controller;

import com.example.Splitfy.catalog.dto.MusicaRequest;
import com.example.Splitfy.catalog.dto.MusicaResponse;
import com.example.Splitfy.catalog.service.MusicaService;
import com.example.Splitfy.historico.dto.RegistrarReproducaoRequisicao;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@RestController
@RequestMapping("/api/musicas")
public class MusicaController {

    private final MusicaService musicaService;

    public MusicaController(MusicaService musicaService) {
        this.musicaService = musicaService;
    }

    @GetMapping
    public List<MusicaResponse> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) Long generoId,
            @RequestParam(required = false) Long albumId
    ) {
        return musicaService.listar(titulo, generoId, albumId);
    }

    @GetMapping("/{id}")
    public MusicaResponse buscar(@PathVariable Long id) {
        return musicaService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<MusicaResponse> criar(@RequestBody MusicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(musicaService.criar(request));
    }

    @PutMapping("/{id}")
    public MusicaResponse atualizar(@PathVariable Long id, @RequestBody MusicaRequest request) {
        return musicaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        musicaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/play")
    @PreAuthorize("#p1 != null and #p1.usuarioId() != null and (hasRole('ADMIN') or #p1.usuarioId().toString() == authentication.name)")
    public MusicaResponse registrarPlay(@PathVariable Long id, @RequestBody RegistrarReproducaoRequisicao requisicao) {
        return musicaService.registrarPlay(id, requisicao.usuarioId());
    }
}
