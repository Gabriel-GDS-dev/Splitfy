package com.example.Splitfy.catalog.service;

import java.net.URI;
import com.example.Splitfy.catalog.exception.DadosInvalidosException;

public final class LinkImagem {
    private LinkImagem() {}
    public static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String url = valor.trim();
        try {
            URI uri = URI.create(url);
            if (url.length() <= 2048 && "https".equals(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null) return url;
        } catch (IllegalArgumentException ignored) {}
        throw new DadosInvalidosException("Use um link HTTPS direto para a imagem, com ate 2048 caracteres.");
    }
}
