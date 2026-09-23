package com.example.Splitfy.catalog.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CatalogValidationException extends RuntimeException {

    private final Map<String, String> fields;

    public CatalogValidationException(String message, Map<String, String> fields) {
        super(message);
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
