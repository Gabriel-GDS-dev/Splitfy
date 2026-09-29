package com.example.Splitfy.playlist.dto;

import java.time.LocalDateTime;

public record PlaylistResponse(
        Long id,
        String nome,
        String descricao,
        boolean publica,
        Long usuarioId,
        LocalDateTime criadoEm
) {
}
