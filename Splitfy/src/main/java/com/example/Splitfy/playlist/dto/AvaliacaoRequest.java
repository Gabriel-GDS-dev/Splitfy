package com.example.Splitfy.playlist.dto;

public record AvaliacaoRequest(
        Long usuarioId,
        Long musicaId,
        Boolean curtido,
        Integer nota
) {
}
