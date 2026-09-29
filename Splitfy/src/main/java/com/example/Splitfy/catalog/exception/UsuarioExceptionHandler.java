package com.example.Splitfy.catalog.exception;

import com.example.Splitfy.catalog.dto.UsuarioErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.Splitfy.catalog.controller")
public class UsuarioExceptionHandler {
    @ExceptionHandler(UsuarioNotFoundException.class)
    public ResponseEntity<UsuarioErrorResponse> notFound(UsuarioNotFoundException e) { return build(HttpStatus.NOT_FOUND, e.getMessage(), Map.of()); }

    @ExceptionHandler(UsuarioValidationException.class)
    public ResponseEntity<UsuarioErrorResponse> validation(UsuarioValidationException e) { return build(HttpStatus.BAD_REQUEST, e.getMessage(), e.getFields()); }

    private ResponseEntity<UsuarioErrorResponse> build(HttpStatus status, String message, Map<String,String> fields) {
        return ResponseEntity.status(status).body(new UsuarioErrorResponse(status.value(), status.getReasonPhrase(), message, fields, Instant.now()));
    }
}
