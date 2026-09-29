package com.example.Splitfy.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ArtistaRequest(
        @NotBlank @Size(max = 150) String nome,
        String biografia,
        @Size(max = 100) String pais,
        @PastOrPresent LocalDate inicioCarreira
) {
}
