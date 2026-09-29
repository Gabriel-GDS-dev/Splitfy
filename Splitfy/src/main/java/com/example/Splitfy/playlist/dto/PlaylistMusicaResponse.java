package com.example.Splitfy.playlist.dto;

import java.time.LocalDateTime;

public record PlaylistMusicaResponse(
        Long id,
        Long playlistId,
        Long musicaId,
        String musicaTitulo,
        LocalDateTime adicionadoEm
) {
}
