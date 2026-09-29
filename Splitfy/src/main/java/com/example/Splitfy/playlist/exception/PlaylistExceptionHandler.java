package com.example.Splitfy.playlist.exception;

import com.example.Splitfy.playlist.dto.PlaylistErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.Splitfy.playlist")
public class PlaylistExceptionHandler {

    @ExceptionHandler(PlaylistNotFoundException.class)
    public ResponseEntity<PlaylistErrorResponse> handleNotFound(PlaylistNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(PlaylistValidationException.class)
    public ResponseEntity<PlaylistErrorResponse> handleValidation(PlaylistValidationException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), exception.getFields());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<PlaylistErrorResponse> handleDataIntegrity() {
        return build(
                HttpStatus.BAD_REQUEST,
                "Operacao invalida. Verifique se os dados ja existem ou as referencias sao validas.",
                Map.of()
        );
    }

    private ResponseEntity<PlaylistErrorResponse> build(
            HttpStatus status,
            String message,
            Map<String, String> fields
    ) {
        PlaylistErrorResponse response = new PlaylistErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                fields,
                Instant.now()
        );
        return ResponseEntity.status(status).body(response);
    }
}
