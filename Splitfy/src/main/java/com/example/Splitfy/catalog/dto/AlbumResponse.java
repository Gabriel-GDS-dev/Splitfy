package com.example.Splitfy.catalog.dto;

import java.time.LocalDateTime;

public record AlbumResponse(
        Long id,
        String titulo,
        Integer anoLancamento,
        String capaUrl,
        Long artistaId,
        String artistaNome,
        LocalDateTime criadoEm
) {
}
