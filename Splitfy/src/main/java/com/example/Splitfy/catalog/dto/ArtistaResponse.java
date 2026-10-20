package com.example.Splitfy.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Artista cadastrado")
public record ArtistaResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Taylor Swift") String nome,
        @Schema(example = "Cantora e compositora norte-americana.") String biografia,
        @Schema(example = "Estados Unidos") String pais,
        @Schema(example = "2006-06-19") LocalDate inicioCarreira,
        @Schema(example = "2026-10-20T14:30:00") LocalDateTime criadoEm
) {
}
