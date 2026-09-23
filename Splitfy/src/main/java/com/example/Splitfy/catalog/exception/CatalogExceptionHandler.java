package com.example.Splitfy.catalog.exception;

import com.example.Splitfy.catalog.dto.CatalogErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.Splitfy.catalog")
public class CatalogExceptionHandler {

    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<CatalogErrorResponse> handleNotFound(CatalogNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CatalogValidationException.class)
    public ResponseEntity<CatalogErrorResponse> handleValidation(CatalogValidationException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), exception.getFields());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CatalogErrorResponse> handleDataIntegrity() {
        return build(
                HttpStatus.BAD_REQUEST,
                "Nao foi possivel salvar. Verifique se album e genero existem e se os dados nao estao duplicados.",
                Map.of()
        );
    }

    private ResponseEntity<CatalogErrorResponse> build(
            HttpStatus status,
            String message,
            Map<String, String> fields
    ) {
        CatalogErrorResponse response = new CatalogErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                fields,
                Instant.now()
        );
        return ResponseEntity.status(status).body(response);
    }
}
