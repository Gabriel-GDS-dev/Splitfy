package com.example.Splitfy.catalog.dto;

public record MusicaRequest(
        String titulo,
        Integer duracaoSegundos,
        Integer numeroFaixa,
        Long albumId,
        Long generoId
) {
}
