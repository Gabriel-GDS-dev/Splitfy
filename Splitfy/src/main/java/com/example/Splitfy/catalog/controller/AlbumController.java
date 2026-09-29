package com.example.Splitfy.catalog.controller;

import com.example.Splitfy.catalog.dto.AlbumRequest;
import com.example.Splitfy.catalog.dto.AlbumResponse;
import com.example.Splitfy.catalog.dto.MusicaResponse;
import com.example.Splitfy.catalog.service.AlbumService;
import com.example.Splitfy.catalog.service.MusicaService;
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

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/albuns")
public class AlbumController {

    private final AlbumService albumService;
    private final MusicaService musicaService;

    public AlbumController(AlbumService albumService, MusicaService musicaService) {
        this.albumService = albumService;
        this.musicaService = musicaService;
    }

    @PostMapping
    public ResponseEntity<AlbumResponse> criar(@RequestBody @Valid AlbumRequest request) {
        AlbumResponse criado = albumService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    public PagedModel<AlbumResponse> listar(@RequestParam(required = false) Integer ano,
                                            @RequestParam(required = false) Long artistaId,
                                            Pageable pageable) {
        return new PagedModel<>(albumService.listar(ano, artistaId, pageable));
    }

    @GetMapping("/{id}")
    public AlbumResponse buscarPorId(@PathVariable Long id) {
        return albumService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public AlbumResponse atualizar(@PathVariable Long id, @RequestBody @Valid AlbumRequest request) {
        return albumService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        albumService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/musicas")
    public List<MusicaResponse> listarMusicas(@PathVariable Long id) {
        return musicaService.listarPorAlbum(id);
    }
}
