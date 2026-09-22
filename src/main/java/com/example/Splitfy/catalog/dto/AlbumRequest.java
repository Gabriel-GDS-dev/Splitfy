package com.example.Splitfy.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// O limite superior de anoLancamento (ano atual) é validado no AlbumService.
public record AlbumRequest(
        @NotBlank @Size(max = 200) String titulo,
        @Min(1900) Integer anoLancamento,
        @Size(max = 500) String capaUrl,
        @NotNull Long artistaId
) {
}
