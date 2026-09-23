package com.example.Splitfy.catalog.dto;

import java.time.LocalDateTime;

public record MusicaResponse(
        Long id,
        String titulo,
        Integer duracaoSegundos,
        Integer numeroFaixa,
        Long reproducoes,
        Long albumId,
        Long generoId,
        String generoNome,
        LocalDateTime criadoEm
) {
}
