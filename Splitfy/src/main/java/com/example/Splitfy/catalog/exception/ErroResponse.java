package com.example.Splitfy.catalog.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Corpo padrão das respostas de erro do catálogo")
public record ErroResponse(
        @Schema(example = "2026-10-20T14:30:00") LocalDateTime timestamp,
        @Schema(example = "404") int status,
        @Schema(example = "Not Found") String erro,
        @Schema(example = "Artista com id 99 não encontrado") String mensagem,
        @Schema(example = "/api/artistas/99") String caminho,
        @Schema(description = "Preenchido apenas em erros de validação") List<CampoErro> errosDeCampo
) {

    public record CampoErro(@Schema(example = "nome") String campo,
                            @Schema(example = "must not be blank") String mensagem) {
    }
}
