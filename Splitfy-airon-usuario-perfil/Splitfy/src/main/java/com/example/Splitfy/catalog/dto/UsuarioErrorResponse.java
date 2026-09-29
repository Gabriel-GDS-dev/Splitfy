package com.example.Splitfy.catalog.dto;

import java.time.Instant;
import java.util.Map;

public record UsuarioErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fields,
        Instant timestamp
) {
}
