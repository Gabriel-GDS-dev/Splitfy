package com.example.Splitfy.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// O limite superior de anoLancamento (ano atual) é validado no AlbumService.
@Schema(description = "Dados para cadastrar ou atualizar um álbum")
public record AlbumRequest(
        @Schema(example = "1989") @NotBlank @Size(max = 200) String titulo,
        @Schema(description = "Entre 1900 e o ano atual", example = "2014") @Min(1900) Integer anoLancamento,
        @Schema(example = "https://exemplo.com/capas/1989.jpg") @Size(max = 2048) String capaUrl,
        @Schema(description = "Id de um artista já cadastrado", example = "1") @NotNull Long artistaId
) {
}
