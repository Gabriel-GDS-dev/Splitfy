package com.example.Splitfy.catalog.service;

import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class BuscaMusicalServiceTest {
    @SuppressWarnings("unchecked")
    private HttpResponse<String> resposta(int status, String body) {
        HttpResponse<String> resposta = mock(HttpResponse.class);
        when(resposta.statusCode()).thenReturn(status);
        when(resposta.body()).thenReturn(body);
        return resposta;
    }

    @Test
    void semCredenciaisExplicaConfiguracaoESemRequisicoesExternas() {
        HttpClient http = mock(HttpClient.class);
        var resultado = new BuscaMusicalService("", "", "", http).buscar("musica cantor");
        assertThat(resultado.resultados()).isEmpty();
        assertThat(resultado.avisos()).singleElement().asString().contains("Vercel");
        assertThat(resultado.spotifyConfigurado()).isFalse();
        verifyNoInteractions(http);
    }

    @Test
    void falhaSpotifyNaoImpedeYoutubeComDuracaoECredenciaisSomenteEmHeaders() throws Exception {
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocacao -> {
            HttpRequest request = invocacao.getArgument(0);
            assertThat(request.uri().toString()).doesNotContain("segredo-google", "segredo-spotify");
            if (request.uri().getHost().equals("accounts.spotify.com")) return resposta(403, "segredo-spotify");
            assertThat(request.headers().firstValue("X-Goog-Api-Key")).contains("segredo-google");
            if (request.uri().getPath().endsWith("/search")) return resposta(200, """
                    {"items":[{"id":{"videoId":"abcdefghijk"},"snippet":{"title":"Musica &amp; cantor","channelTitle":"Canal","thumbnails":{"medium":{"url":"https://i.ytimg.com/capa.jpg"}}}}]}
                    """);
            return resposta(200, "{\"items\":[{\"id\":\"abcdefghijk\",\"contentDetails\":{\"duration\":\"PT3M20S\"}}]}");
        });
        var resultado = new BuscaMusicalService("id", "segredo-spotify", "segredo-google", http).buscar("musica cantor");
        assertThat(resultado.resultados()).hasSize(1);
        assertThat(resultado.resultados().getFirst().duracaoSegundos()).isEqualTo(200);
        assertThat(resultado.resultados().getFirst().titulo()).isEqualTo("Musica & cantor");
        assertThat(resultado.avisos()).singleElement().asString().contains("Spotify").doesNotContain("segredo");
    }

    @Test
    void buscaSpotifyNormalizaMetadadosEReutilizaTokenECache() throws Exception {
        HttpClient http = mock(HttpClient.class);
        AtomicInteger chamadas = new AtomicInteger();
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocacao -> {
            chamadas.incrementAndGet();
            HttpRequest request = invocacao.getArgument(0);
            if (request.uri().getHost().equals("accounts.spotify.com")) return resposta(200, "{\"access_token\":\"token-teste\",\"expires_in\":3600}");
            assertThat(request.headers().firstValue("Authorization")).contains("Bearer token-teste");
            return resposta(200, """
                    {"tracks":{"items":[{"id":"1234567890123456789012","name":"Faixa","artists":[{"name":"Cantor"}],"duration_ms":210000,"album":{"name":"Album","images":[{"url":"https://i.scdn.co/capa"}]}}]}}
                    """);
        });
        var busca = new BuscaMusicalService("id", "segredo", "", http);
        var resultado = busca.buscar("faixa cantor");
        assertThat(resultado.resultados().getFirst().duracaoSegundos()).isEqualTo(210);
        assertThat(resultado.resultados().getFirst().album()).isEqualTo("Album");
        assertThat(busca.buscar("faixa cantor")).isEqualTo(resultado);
        assertThat(chamadas.get()).isEqualTo(2);
        busca.buscar("outra faixa");
        assertThat(chamadas.get()).isEqualTo(3);
    }

    @Test
    void linksAceitamSomenteFaixasDeHostsOficiais() {
        assertThat(LinksMusicais.spotify("https://open.spotify.com/intl-pt/track/1234567890123456789012?si=abc"))
                .isEqualTo("https://open.spotify.com/track/1234567890123456789012");
        assertThat(LinksMusicais.youtube("https://music.youtube.com/watch?v=abcdefghijk&list=PLabc"))
                .isEqualTo("https://www.youtube.com/watch?v=abcdefghijk");
        assertThat(LinksMusicais.youtube("https://youtu.be/abcdefghijk?si=abc")).endsWith("v=abcdefghijk");
        for (String url : new String[]{"https://youtube.com.evil.test/watch?v=abcdefghijk", "javascript:alert(1)", "https://user@youtube.com/watch?v=abcdefghijk"}) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> LinksMusicais.youtube(url))
                    .isInstanceOf(com.example.Splitfy.catalog.exception.CatalogValidationException.class);
        }
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> LinksMusicais.spotify("https://open.spotify.com/playlist/1234567890123456789012"))
                .isInstanceOf(com.example.Splitfy.catalog.exception.CatalogValidationException.class);
    }
}
