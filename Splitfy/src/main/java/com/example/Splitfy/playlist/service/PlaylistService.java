package com.example.Splitfy.playlist.service;

import com.example.Splitfy.catalog.entity.Musica;
import com.example.Splitfy.catalog.repository.MusicaRepository;
import com.example.Splitfy.playlist.dto.AdicionarMusicaRequest;
import com.example.Splitfy.playlist.dto.PlaylistMusicaResponse;
import com.example.Splitfy.playlist.dto.PlaylistRequest;
import com.example.Splitfy.playlist.dto.PlaylistResponse;
import com.example.Splitfy.playlist.entity.Playlist;
import com.example.Splitfy.playlist.entity.PlaylistMusica;
import com.example.Splitfy.playlist.exception.PlaylistNotFoundException;
import com.example.Splitfy.playlist.exception.PlaylistValidationException;
import com.example.Splitfy.playlist.repository.PlaylistMusicaRepository;
import com.example.Splitfy.playlist.repository.PlaylistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistMusicaRepository playlistMusicaRepository;
    private final MusicaRepository musicaRepository;

    public PlaylistService(PlaylistRepository playlistRepository,
                           PlaylistMusicaRepository playlistMusicaRepository,
                           MusicaRepository musicaRepository) {
        this.playlistRepository = playlistRepository;
        this.playlistMusicaRepository = playlistMusicaRepository;
        this.musicaRepository = musicaRepository;
    }

    @Transactional(readOnly = true)
    public List<PlaylistResponse> listar() {
        return playlistRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaylistResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<PlaylistResponse> listarPorUsuario(Long usuarioId) {
        if (!playlistRepository.usuarioExiste(usuarioId)) {
            throw new PlaylistNotFoundException("Usuario nao encontrado.");
        }
        return playlistRepository.findByUsuarioIdOrderByNomeAsc(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PlaylistResponse criar(PlaylistRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        String nome = limparTexto(request.nome());
        if (nome == null) {
            campos.put("nome", "Nome da playlist e obrigatorio.");
        }
        if (request.usuarioId() == null || request.usuarioId() <= 0) {
            campos.put("usuarioId", "Usuario e obrigatorio.");
        } else if (!playlistRepository.usuarioExiste(request.usuarioId())) {
            campos.put("usuarioId", "Usuario nao encontrado.");
        }
        if (!campos.isEmpty()) {
            throw new PlaylistValidationException("Dados de playlist invalidos.", campos);
        }
        boolean publica = request.publica() != null && request.publica();
        Playlist playlist = new Playlist(nome, limparTexto(request.descricao()), publica, request.usuarioId());
        return toResponse(playlistRepository.save(playlist));
    }

    @Transactional
    public PlaylistResponse atualizar(Long id, PlaylistRequest request) {
        Playlist playlist = buscarEntidade(id);
        Map<String, String> campos = new LinkedHashMap<>();
        String nome = limparTexto(request.nome());
        if (nome == null) {
            campos.put("nome", "Nome da playlist e obrigatorio.");
        }
        if (request.usuarioId() == null || !request.usuarioId().equals(playlist.getUsuarioId())) {
            campos.put("usuarioId", "Apenas o dono da playlist pode edita-la.");
        }
        if (!campos.isEmpty()) {
            throw new PlaylistValidationException("Dados de playlist invalidos.", campos);
        }
        playlist.setNome(nome);
        playlist.setDescricao(limparTexto(request.descricao()));
        playlist.setPublica(request.publica() != null && request.publica());
        return toResponse(playlistRepository.save(playlist));
    }

    @Transactional
    public void excluir(Long id, Long usuarioId) {
        Playlist playlist = buscarEntidade(id);
        if (usuarioId == null || !usuarioId.equals(playlist.getUsuarioId())) {
            throw new PlaylistValidationException(
                    "Apenas o dono da playlist pode exclui-la.",
                    Map.of("usuarioId", "Usuario nao autorizado.")
            );
        }
        playlistRepository.delete(playlist);
    }

    @Transactional(readOnly = true)
    public List<PlaylistMusicaResponse> listarMusicas(Long playlistId) {
        buscarEntidade(playlistId);
        return playlistMusicaRepository.findByPlaylistId(playlistId).stream()
                .map(this::toMusicaResponse)
                .toList();
    }

    @Transactional
    public PlaylistMusicaResponse adicionarMusica(Long playlistId, AdicionarMusicaRequest request) {
        Playlist playlist = buscarEntidade(playlistId);
        Map<String, String> campos = new LinkedHashMap<>();
        if (request.usuarioId() == null || !request.usuarioId().equals(playlist.getUsuarioId())) {
            campos.put("usuarioId", "Apenas o dono da playlist pode adicionar musicas.");
        }
        if (request.musicaId() == null || request.musicaId() <= 0) {
            campos.put("musicaId", "Musica e obrigatoria.");
        }
        if (!campos.isEmpty()) {
            throw new PlaylistValidationException("Dados invalidos.", campos);
        }
        Musica musica = musicaRepository.findById(request.musicaId())
                .orElseThrow(() -> new PlaylistNotFoundException("Musica nao encontrada."));
        if (playlistMusicaRepository.existsByPlaylistIdAndMusicaId(playlistId, request.musicaId())) {
            throw new PlaylistValidationException(
                    "Musica ja esta na playlist.",
                    Map.of("musicaId", "Musica duplicada na playlist.")
            );
        }
        PlaylistMusica pm = new PlaylistMusica(playlist, musica);
        return toMusicaResponse(playlistMusicaRepository.save(pm));
    }

    @Transactional
    public void removerMusica(Long playlistId, Long musicaId, Long usuarioId) {
        Playlist playlist = buscarEntidade(playlistId);
        if (usuarioId == null || !usuarioId.equals(playlist.getUsuarioId())) {
            throw new PlaylistValidationException(
                    "Apenas o dono da playlist pode remover musicas.",
                    Map.of("usuarioId", "Usuario nao autorizado.")
            );
        }
        PlaylistMusica pm = playlistMusicaRepository.findByPlaylistIdAndMusicaId(playlistId, musicaId)
                .orElseThrow(() -> new PlaylistNotFoundException("Musica nao encontrada na playlist."));
        playlistMusicaRepository.delete(pm);
    }

    private Playlist buscarEntidade(Long id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> new PlaylistNotFoundException("Playlist nao encontrada."));
    }

    private String limparTexto(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        return valor.trim();
    }

    private PlaylistResponse toResponse(Playlist playlist) {
        return new PlaylistResponse(
                playlist.getId(),
                playlist.getNome(),
                playlist.getDescricao(),
                playlist.isPublica(),
                playlist.getUsuarioId(),
                playlist.getCriadoEm()
        );
    }

    private PlaylistMusicaResponse toMusicaResponse(PlaylistMusica pm) {
        return new PlaylistMusicaResponse(
                pm.getId(),
                pm.getPlaylist().getId(),
                pm.getMusica().getId(),
                pm.getMusica().getTitulo(),
                pm.getAdicionadoEm()
        );
    }
}
