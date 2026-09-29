package com.example.Splitfy.catalog.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ArtistaResponse(
        Long id,
        String nome,
        String biografia,
        String pais,
        LocalDate inicioCarreira,
        LocalDateTime criadoEm
) {
}
