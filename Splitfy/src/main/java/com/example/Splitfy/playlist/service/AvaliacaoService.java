package com.example.Splitfy.playlist.service;

import com.example.Splitfy.catalog.entity.Musica;
import com.example.Splitfy.catalog.repository.MusicaRepository;
import com.example.Splitfy.playlist.dto.AvaliacaoRequest;
import com.example.Splitfy.playlist.dto.AvaliacaoResponse;
import com.example.Splitfy.playlist.entity.Avaliacao;
import com.example.Splitfy.playlist.exception.PlaylistNotFoundException;
import com.example.Splitfy.playlist.exception.PlaylistValidationException;
import com.example.Splitfy.playlist.repository.AvaliacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final MusicaRepository musicaRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, MusicaRepository musicaRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.musicaRepository = musicaRepository;
    }

    @Transactional(readOnly = true)
    public AvaliacaoResponse buscar(Long usuarioId, Long musicaId) {
        return toResponse(buscarEntidade(usuarioId, musicaId));
    }

    @Transactional
    public AvaliacaoResponse criar(AvaliacaoRequest request) {
        Map<String, String> campos = validarCamposObrigatorios(request);
        if (request.usuarioId() != null && request.usuarioId() > 0
                && !avaliacaoRepository.usuarioExiste(request.usuarioId())) {
            campos.put("usuarioId", "Usuario nao encontrado.");
        }
        if (!campos.isEmpty()) {
            throw new PlaylistValidationException("Dados de avaliacao invalidos.", campos);
        }
        if (avaliacaoRepository.existsByUsuarioIdAndMusicaId(request.usuarioId(), request.musicaId())) {
            throw new PlaylistValidationException(
                    "Avaliacao duplicada.",
                    Map.of("avaliacao", "Ja existe uma avaliacao desse usuario para essa musica.")
            );
        }
        Musica musica = musicaRepository.findById(request.musicaId())
                .orElseThrow(() -> new PlaylistNotFoundException("Musica nao encontrada."));
        boolean curtido = request.curtido() != null && request.curtido();
        Avaliacao avaliacao = new Avaliacao(request.usuarioId(), musica, curtido, request.nota());
        return toResponse(avaliacaoRepository.save(avaliacao));
    }

    @Transactional
    public AvaliacaoResponse atualizar(Long usuarioId, Long musicaId, AvaliacaoRequest request) {
        Avaliacao avaliacao = buscarEntidade(usuarioId, musicaId);
        if (request.nota() != null && (request.nota() < 1 || request.nota() > 5)) {
            throw new PlaylistValidationException(
                    "Dados de avaliacao invalidos.",
                    Map.of("nota", "Nota deve estar entre 1 e 5.")
            );
        }
        if (request.curtido() != null) {
            avaliacao.setCurtido(request.curtido());
        }
        if (request.nota() != null) {
            avaliacao.setNota(request.nota());
        }
        return toResponse(avaliacaoRepository.save(avaliacao));
    }

    @Transactional
    public void excluir(Long usuarioId, Long musicaId) {
        Avaliacao avaliacao = buscarEntidade(usuarioId, musicaId);
        avaliacaoRepository.delete(avaliacao);
    }

    @Transactional(readOnly = true)
    public long contarCurtidas(Long musicaId) {
        if (!musicaRepository.existsById(musicaId)) {
            throw new PlaylistNotFoundException("Musica nao encontrada.");
        }
        return avaliacaoRepository.contarCurtidas(musicaId);
    }

    private Avaliacao buscarEntidade(Long usuarioId, Long musicaId) {
        return avaliacaoRepository.findByUsuarioIdAndMusicaId(usuarioId, musicaId)
                .orElseThrow(() -> new PlaylistNotFoundException("Avaliacao nao encontrada."));
    }

    private Map<String, String> validarCamposObrigatorios(AvaliacaoRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        if (request.usuarioId() == null || request.usuarioId() <= 0) {
            campos.put("usuarioId", "Usuario e obrigatorio.");
        }
        if (request.musicaId() == null || request.musicaId() <= 0) {
            campos.put("musicaId", "Musica e obrigatoria.");
        }
        if (request.nota() != null && (request.nota() < 1 || request.nota() > 5)) {
            campos.put("nota", "Nota deve estar entre 1 e 5.");
        }
        return campos;
    }

    private AvaliacaoResponse toResponse(Avaliacao avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.getId(),
                avaliacao.getUsuarioId(),
                avaliacao.getMusica().getId(),
                avaliacao.isCurtido(),
                avaliacao.getNota(),
                avaliacao.getCriadoEm()
        );
    }
}
