package com.example.Splitfy.catalog.exception;

import java.util.Map;

public class UsuarioValidationException extends RuntimeException {
    private final Map<String, String> fields;
    public UsuarioValidationException(String message, Map<String, String> fields) {
        super(message);
        this.fields = fields;
    }
    public Map<String, String> getFields() { return fields; }
}
