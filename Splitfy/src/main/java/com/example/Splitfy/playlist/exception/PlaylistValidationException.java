package com.example.Splitfy.playlist.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlaylistValidationException extends RuntimeException {

    private final Map<String, String> fields;

    public PlaylistValidationException(String message, Map<String, String> fields) {
        super(message);
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
