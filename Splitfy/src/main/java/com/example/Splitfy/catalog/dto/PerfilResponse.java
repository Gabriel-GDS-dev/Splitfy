package com.example.Splitfy.catalog.dto;

import java.time.LocalDateTime;

public record PerfilResponse(
        Long id,
        Long usuarioId,
        String nomeExibicao,
        String bio,
        String fotoUrl,
        LocalDateTime criadoEm
) {
}
