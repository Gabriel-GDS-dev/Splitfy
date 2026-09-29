package com.example.Splitfy.playlist.dto;

import java.time.Instant;
import java.util.Map;

public record PlaylistErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fields,
        Instant timestamp
) {
}
