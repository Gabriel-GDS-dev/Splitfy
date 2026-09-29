package com.example.Splitfy.playlist.dto;

public record PlaylistRequest(
        String nome,
        String descricao,
        Boolean publica,
        Long usuarioId
) {
}
