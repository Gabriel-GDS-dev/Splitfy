package com.example.Splitfy.catalog.controller;

import com.example.Splitfy.catalog.dto.GeneroRequest;
import com.example.Splitfy.catalog.dto.GeneroResponse;
import com.example.Splitfy.catalog.dto.MusicaResponse;
import com.example.Splitfy.catalog.service.GeneroService;
import com.example.Splitfy.catalog.service.MusicaService;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@RestController
@RequestMapping("/api/generos")
public class GeneroController {

    private final GeneroService generoService;
    private final MusicaService musicaService;

    public GeneroController(GeneroService generoService, MusicaService musicaService) {
        this.generoService = generoService;
        this.musicaService = musicaService;
    }

    @GetMapping
    public List<GeneroResponse> listar() {
        return generoService.listar();
    }

    @GetMapping("/{id}")
    public GeneroResponse buscar(@PathVariable Long id) {
        return generoService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<GeneroResponse> criar(@RequestBody GeneroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(generoService.criar(request));
    }

    @PutMapping("/{id}")
    public GeneroResponse atualizar(@PathVariable Long id, @RequestBody GeneroRequest request) {
        return generoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        generoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/musicas")
    public List<MusicaResponse> listarMusicasPorGenero(@PathVariable Long id) {
        return musicaService.listarPorGenero(id);
    }
}
