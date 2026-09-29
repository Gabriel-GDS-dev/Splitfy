package com.example.Splitfy.playlist.controller;

import com.example.Splitfy.playlist.dto.AdicionarMusicaRequest;
import com.example.Splitfy.playlist.dto.PlaylistMusicaResponse;
import com.example.Splitfy.playlist.dto.PlaylistRequest;
import com.example.Splitfy.playlist.dto.PlaylistResponse;
import com.example.Splitfy.playlist.service.PlaylistService;
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

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping
    public List<PlaylistResponse> listar() {
        return playlistService.listar();
    }

    @GetMapping("/{id}")
    public PlaylistResponse buscar(@PathVariable Long id) {
        return playlistService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<PlaylistResponse> criar(@RequestBody PlaylistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistService.criar(request));
    }

    @PutMapping("/{id}")
    public PlaylistResponse atualizar(@PathVariable Long id, @RequestBody PlaylistRequest request) {
        return playlistService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @RequestParam Long usuarioId) {
        playlistService.excluir(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/musicas")
    public List<PlaylistMusicaResponse> listarMusicas(@PathVariable Long id) {
        return playlistService.listarMusicas(id);
    }

    @PostMapping("/{id}/musicas")
    public ResponseEntity<PlaylistMusicaResponse> adicionarMusica(
            @PathVariable Long id,
            @RequestBody AdicionarMusicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistService.adicionarMusica(id, request));
    }

    @DeleteMapping("/{id}/musicas/{musicaId}")
    public ResponseEntity<Void> removerMusica(
            @PathVariable Long id,
            @PathVariable Long musicaId,
            @RequestParam Long usuarioId) {
        playlistService.removerMusica(id, musicaId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
