package com.example.Splitfy.playlist.controller;

import com.example.Splitfy.playlist.dto.PlaylistResponse;
import com.example.Splitfy.playlist.service.PlaylistService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios/{usuarioId}/playlists")
public class UsuarioPlaylistController {

    private final PlaylistService playlistService;

    public UsuarioPlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping
    public List<PlaylistResponse> listarPorUsuario(@PathVariable Long usuarioId) {
        return playlistService.listarPorUsuario(usuarioId);
    }
}
