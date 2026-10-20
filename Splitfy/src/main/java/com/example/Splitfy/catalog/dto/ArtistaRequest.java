package com.example.Splitfy.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Dados para cadastrar ou atualizar um artista")
public record ArtistaRequest(
        @Schema(example = "Taylor Swift") @NotBlank @Size(max = 150) String nome,
        @Schema(example = "Cantora e compositora norte-americana.") String biografia,
        @Schema(example = "Estados Unidos") @Size(max = 100) String pais,
        @Schema(description = "Não pode ser uma data futura", example = "2006-06-19") @PastOrPresent LocalDate inicioCarreira
) {
}
