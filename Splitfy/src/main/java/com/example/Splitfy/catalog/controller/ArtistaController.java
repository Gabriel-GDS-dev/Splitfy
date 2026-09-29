package com.example.Splitfy.catalog.controller;

import com.example.Splitfy.catalog.dto.AlbumResponse;
import com.example.Splitfy.catalog.dto.ArtistaRequest;
import com.example.Splitfy.catalog.dto.ArtistaResponse;
import com.example.Splitfy.catalog.service.AlbumService;
import com.example.Splitfy.catalog.service.ArtistaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;

@RestController
@RequestMapping("/api/artistas")
public class ArtistaController {

    private final ArtistaService artistaService;
    private final AlbumService albumService;

    public ArtistaController(ArtistaService artistaService, AlbumService albumService) {
        this.artistaService = artistaService;
        this.albumService = albumService;
    }

    @PostMapping
    public ResponseEntity<ArtistaResponse> criar(@RequestBody @Valid ArtistaRequest request) {
        ArtistaResponse criado = artistaService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    public PagedModel<ArtistaResponse> listar(@RequestParam(required = false) String nome, Pageable pageable) {
        return new PagedModel<>(artistaService.listar(nome, pageable));
    }

    @GetMapping("/{id}")
    public ArtistaResponse buscarPorId(@PathVariable Long id) {
        return artistaService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ArtistaResponse atualizar(@PathVariable Long id, @RequestBody @Valid ArtistaRequest request) {
        return artistaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        artistaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/albuns")
    public PagedModel<AlbumResponse> listarAlbuns(@PathVariable Long id, Pageable pageable) {
        return new PagedModel<>(albumService.listarPorArtista(id, pageable));
    }
}
