package com.example.Splitfy.catalog.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class BuscaMusicalService {
    public record Resultado(String fonte, String titulo, String artista, String album, String url, String capaUrl, Integer duracaoSegundos) {}
    public record Resposta(List<Resultado> resultados, List<String> avisos, boolean spotifyConfigurado, boolean youtubeConfigurado) {}
    private record Cache(long ate, Resposta resposta) {}
    private final String clientId, clientSecret, youtubeKey;
    private final HttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Map<String, Cache> cache = Collections.synchronizedMap(new LinkedHashMap<>());
    private String token;
    private long tokenAte;

    @org.springframework.beans.factory.annotation.Autowired
    public BuscaMusicalService(@Value("${SPOTIFY_CLIENT_ID:}") String clientId,
                              @Value("${SPOTIFY_CLIENT_SECRET:}") String clientSecret,
                              @Value("${YOUTUBE_API_KEY:}") String youtubeKey) {
        this(clientId, clientSecret, youtubeKey, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build());
    }

    BuscaMusicalService(String clientId, String clientSecret, String youtubeKey, HttpClient http) {
        this.clientId = clientId; this.clientSecret = clientSecret; this.youtubeKey = youtubeKey; this.http = http;
    }

    public Resposta status() { return new Resposta(List.of(), List.of(), !clientId.isBlank() && !clientSecret.isBlank(), !youtubeKey.isBlank()); }

    public Resposta buscar(String consulta) {
        Cache anterior = cache.get(consulta);
        if (anterior != null && anterior.ate > System.currentTimeMillis()) return anterior.resposta;
        Resposta config = status();
        List<String> avisos = Collections.synchronizedList(new ArrayList<>());
        var spotify = CompletableFuture.supplyAsync(() -> {
            if (!config.spotifyConfigurado) return List.<Resultado>of();
            try { return spotify(consulta); }
            catch (Exception e) { avisos.add("Spotify indisponivel. Confira as credenciais e a permissao do aplicativo."); return List.<Resultado>of(); }
        });
        var youtube = CompletableFuture.supplyAsync(() -> {
            if (!config.youtubeConfigurado) return List.<Resultado>of();
            try { return youtube(consulta); }
            catch (Exception e) { avisos.add("YouTube indisponivel. Confira a chave da API e a cota do projeto."); return List.<Resultado>of(); }
        });
        List<Resultado> resultados = new ArrayList<>(spotify.join());
        resultados.addAll(youtube.join());
        if (!config.spotifyConfigurado && !config.youtubeConfigurado) avisos.add("A busca por nome aguarda a configuracao das credenciais na Vercel. Enquanto isso, cole o link da musica abaixo ou envie um MP3.");
        Resposta resposta = new Resposta(List.copyOf(resultados), List.copyOf(avisos), config.spotifyConfigurado, config.youtubeConfigurado);
        if (!resultados.isEmpty() && avisos.isEmpty()) {
            synchronized (cache) {
                if (cache.size() >= 100) cache.remove(cache.keySet().iterator().next());
                cache.put(consulta, new Cache(System.currentTimeMillis() + 300_000, resposta));
            }
        }
        return resposta;
    }

    private List<Resultado> spotify(String consulta) throws Exception {
        JsonNode resposta = enviar(HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/search?type=track&limit=5&q=" + enc(consulta)))
                .header("Authorization", "Bearer " + spotifyToken()).GET());
        List<Resultado> resultados = new ArrayList<>();
        for (JsonNode faixa : resposta.path("tracks").path("items")) {
            List<String> artistas = new ArrayList<>();
            for (JsonNode artista : faixa.path("artists")) artistas.add(artista.path("name").asText());
            resultados.add(new Resultado("SPOTIFY", faixa.path("name").asText(), String.join(", ", artistas), faixa.path("album").path("name").asText(),
                    "https://open.spotify.com/track/" + faixa.path("id").asText(),
                    faixa.path("album").path("images").path(0).path("url").asText(""),
                    Math.max(1, faixa.path("duration_ms").asInt() / 1000)));
        }
        return resultados;
    }

    private synchronized String spotifyToken() throws Exception {
        if (token != null && tokenAte > System.currentTimeMillis()) return token;
        String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        JsonNode resposta = enviar(HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
                .header("Authorization", "Basic " + basic).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials")));
        token = resposta.path("access_token").asText();
        if (token.isBlank()) throw new IllegalStateException("Token indisponivel");
        tokenAte = System.currentTimeMillis() + Math.max(1, resposta.path("expires_in").asInt(3600) - 60) * 1000L;
        return token;
    }

    private List<Resultado> youtube(String consulta) throws Exception {
        JsonNode resposta = enviar(HttpRequest.newBuilder(URI.create("https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoEmbeddable=true&videoCategoryId=10&maxResults=5&q=" + enc(consulta)))
                .header("X-Goog-Api-Key", youtubeKey).GET());
        List<Resultado> resultados = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (JsonNode faixa : resposta.path("items")) ids.add(faixa.path("id").path("videoId").asText());
        if (ids.isEmpty()) return resultados;
        Map<String, Integer> duracoes = new HashMap<>();
        JsonNode videos = enviar(HttpRequest.newBuilder(URI.create("https://www.googleapis.com/youtube/v3/videos?part=contentDetails&id=" + enc(String.join(",", ids))))
                .header("X-Goog-Api-Key", youtubeKey).GET());
        for (JsonNode video : videos.path("items")) {
            String duracao = video.path("contentDetails").path("duration").asText();
            try { duracoes.put(video.path("id").asText(), Math.max(1, (int) Duration.parse(duracao).getSeconds())); }
            catch (Exception ignored) {}
        }
        for (JsonNode faixa : resposta.path("items")) {
            JsonNode snippet = faixa.path("snippet");
            String id = faixa.path("id").path("videoId").asText();
            resultados.add(new Resultado("YOUTUBE", org.springframework.web.util.HtmlUtils.htmlUnescape(snippet.path("title").asText()),
                    org.springframework.web.util.HtmlUtils.htmlUnescape(snippet.path("channelTitle").asText()), org.springframework.web.util.HtmlUtils.htmlUnescape(snippet.path("title").asText()),
                    "https://www.youtube.com/watch?v=" + id, snippet.path("thumbnails").path("medium").path("url").asText(""), duracoes.get(id)));
        }
        return resultados;
    }

    private JsonNode enviar(HttpRequest.Builder builder) throws Exception {
        HttpResponse<String> resposta = http.send(builder.timeout(Duration.ofSeconds(6)).header("Accept", "application/json").build(), HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) throw new IllegalStateException("Servico musical indisponivel");
        return json.readTree(resposta.body());
    }

    private static String enc(String valor) { return URLEncoder.encode(valor, StandardCharsets.UTF_8); }
}
