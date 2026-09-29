package com.example.Splitfy.playlist.dto;

import java.time.LocalDateTime;

public record AvaliacaoResponse(
        Long id,
        Long usuarioId,
        Long musicaId,
        boolean curtido,
        Integer nota,
        LocalDateTime criadoEm
) {
}
