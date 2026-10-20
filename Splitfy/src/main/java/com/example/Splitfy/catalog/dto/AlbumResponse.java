package com.example.Splitfy.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Álbum cadastrado")
public record AlbumResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1989") String titulo,
        @Schema(example = "2014") Integer anoLancamento,
        @Schema(example = "https://exemplo.com/capas/1989.jpg") String capaUrl,
        @Schema(example = "1") Long artistaId,
        @Schema(example = "Taylor Swift") String artistaNome,
        @Schema(example = "2026-10-20T14:30:00") LocalDateTime criadoEm
) {
}
