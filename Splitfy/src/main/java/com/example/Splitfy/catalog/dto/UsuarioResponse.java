package com.example.Splitfy.catalog.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        LocalDate dataNascimento,
        String role,
        boolean ativo,
        LocalDateTime criadoEm
) {
}
