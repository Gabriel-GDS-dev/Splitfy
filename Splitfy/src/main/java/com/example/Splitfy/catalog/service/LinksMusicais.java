package com.example.Splitfy.catalog.service;

import com.example.Splitfy.catalog.exception.CatalogValidationException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class LinksMusicais {
    private LinksMusicais() {}

    public static String spotify(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String texto = valor.trim();
        if (texto.matches("spotify:track:[A-Za-z0-9]{22}")) {
            return "https://open.spotify.com/track/" + texto.substring(14);
        }
        try {
            URI uri = URI.create(texto);
            if ("https".equals(uri.getScheme()) && "open.spotify.com".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null && uri.getPort() == -1
                    && uri.getPath().matches("/(intl-[a-z]{2}/)?track/[A-Za-z0-9]{22}/?")) {
                String path = uri.getPath().replaceAll("/$", "");
                return "https://open.spotify.com/track/" + path.substring(path.lastIndexOf('/') + 1);
            }
        } catch (IllegalArgumentException ignored) {}
        throw invalido("spotifyUrl", "Use o link de uma faixa do Spotify (open.spotify.com/track/...).");
    }

    public static String youtube(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            URI uri = URI.create(valor.trim());
            String host = uri.getHost();
            String id = null;
            if (!"https".equals(uri.getScheme()) || host == null || uri.getUserInfo() != null || uri.getPort() != -1) throw new IllegalArgumentException();
            if (host.equalsIgnoreCase("youtu.be")) id = uri.getPath().substring(1);
            else if (java.util.Set.of("youtube.com", "www.youtube.com", "music.youtube.com", "m.youtube.com").contains(host.toLowerCase(java.util.Locale.ROOT))) {
                if (uri.getPath().equals("/watch") && uri.getRawQuery() != null) {
                    for (String item : uri.getRawQuery().split("&")) {
                        String[] par = item.split("=", 2);
                        if (par.length == 2 && par[0].equals("v")) id = URLDecoder.decode(par[1], StandardCharsets.UTF_8);
                    }
                } else if (uri.getPath().matches("/(shorts|embed)/[A-Za-z0-9_-]{11}")) {
                    id = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
                }
            }
            if (id != null && id.matches("[A-Za-z0-9_-]{11}")) return "https://www.youtube.com/watch?v=" + id;
        } catch (IllegalArgumentException ignored) {}
        throw invalido("youtubeUrl", "Use o link de uma musica do YouTube Music ou de um video do YouTube.");
    }

    public static String preferida(String valor) {
        String fonte = valor == null || valor.isBlank() ? "AUTO" : valor.trim().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("AUTO", "SPOTIFY", "YOUTUBE", "MP3").contains(fonte)) throw invalido("fontePreferida", "Fonte de reproducao invalida.");
        return fonte;
    }

    private static CatalogValidationException invalido(String campo, String mensagem) {
        return new CatalogValidationException(mensagem, Map.of(campo, mensagem));
    }
}
