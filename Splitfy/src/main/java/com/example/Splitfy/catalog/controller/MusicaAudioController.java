package com.example.Splitfy.catalog.controller;

import com.example.Splitfy.catalog.exception.CatalogNotFoundException;
import com.example.Splitfy.catalog.exception.CatalogValidationException;
import com.example.Splitfy.catalog.repository.MusicaRepository;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/musicas/{id}/audio")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sem permissao")
})
public class MusicaAudioController {
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private final JdbcTemplate jdbc;
    private final MusicaRepository musicas;

    public MusicaAudioController(JdbcTemplate jdbc, MusicaRepository musicas) {
        this.jdbc = jdbc;
        this.musicas = musicas;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Enviar ou substituir MP3 da musica")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> enviar(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo)
            throws IOException {
        if (!musicas.existsById(id)) throw new CatalogNotFoundException("Musica nao encontrada.");
        String nome = arquivo.getOriginalFilename();
        if (arquivo.isEmpty() || arquivo.getSize() > MAX_BYTES || nome == null
                || !nome.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
            throw invalido();
        }
        byte[] bytes = arquivo.getBytes();
        boolean id3 = bytes.length >= 10 && bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3';
        boolean mpeg = bytes.length >= 4 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xe0) == 0xe0 && (bytes[1] & 0x06) == 0x02
                && (bytes[2] & 0xf0) != 0 && (bytes[2] & 0xf0) != 0xf0;
        if (!id3 && !mpeg) throw invalido();
        // Lock the parent so concurrent replacements cannot create duplicate audio rows.
        jdbc.queryForObject("SELECT id FROM musica WHERE id = ? FOR UPDATE", Long.class, id);
        jdbc.update("DELETE FROM musica_audio WHERE musica_id = ?", id);
        jdbc.update("INSERT INTO musica_audio (musica_id, conteudo) VALUES (?, ?)", id, bytes);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(produces = "audio/mpeg")
    @io.swagger.v3.oas.annotations.Operation(summary = "Ouvir MP3 da musica")
    public ResponseEntity<ByteArrayResource> ouvir(@PathVariable Long id) {
        var resultados = jdbc.query("SELECT conteudo FROM musica_audio WHERE musica_id = ?",
                (rs, linha) -> rs.getBytes(1), id);
        if (resultados.isEmpty()) throw new CatalogNotFoundException("Esta musica ainda nao possui MP3. Edite a musica para enviar o arquivo.");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/mpeg"))
                .cacheControl(CacheControl.noStore())
                .body(new ByteArrayResource(resultados.getFirst()));
    }

    private CatalogValidationException invalido() {
        return new CatalogValidationException("Envie um arquivo MP3 valido de ate 4 MB.", Map.of());
    }
}
