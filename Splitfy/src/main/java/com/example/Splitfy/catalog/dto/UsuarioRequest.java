package com.example.Splitfy.catalog.dto;

public record UsuarioRequest(
        String nome,
        String email,
        String senha,
        String dataNascimento,
        String role
) {
}
