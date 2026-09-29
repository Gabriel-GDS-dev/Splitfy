package com.example.Splitfy.catalog.service;

import com.example.Splitfy.catalog.dto.MusicaRequest;
import com.example.Splitfy.catalog.dto.MusicaResponse;
import com.example.Splitfy.catalog.entity.Genero;
import com.example.Splitfy.catalog.entity.Musica;
import com.example.Splitfy.catalog.exception.CatalogNotFoundException;
import com.example.Splitfy.catalog.exception.CatalogValidationException;
import com.example.Splitfy.catalog.repository.GeneroRepository;
import com.example.Splitfy.catalog.repository.MusicaRepository;
import com.example.Splitfy.historico.service.HistoricoReproducaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MusicaService {

    private final MusicaRepository musicaRepository;
    private final GeneroRepository generoRepository;
    private final HistoricoReproducaoService historicoService;

    public MusicaService(MusicaRepository musicaRepository, GeneroRepository generoRepository,
                         HistoricoReproducaoService historicoService) {
        this.musicaRepository = musicaRepository;
        this.generoRepository = generoRepository;
        this.historicoService = historicoService;
    }

    @Transactional(readOnly = true)
    public List<MusicaResponse> listar(String titulo, Long generoId, Long albumId) {
        return musicaRepository.search(limparFiltro(titulo), generoId, albumId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MusicaResponse> listarPorGenero(Long generoId) {
        if (!generoRepository.existsById(generoId)) {
            throw new CatalogNotFoundException("Genero nao encontrado.");
        }
        return musicaRepository.findByGeneroIdOrderByTituloAsc(generoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MusicaResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public MusicaResponse criar(MusicaRequest request) {
        validar(request);
        Genero genero = buscarGenero(request.generoId());
        Musica musica = new Musica(
                request.titulo().trim(),
                request.duracaoSegundos(),
                request.numeroFaixa(),
                request.albumId(),
                genero
        );
        return toResponse(musicaRepository.save(musica));
    }

    @Transactional
    public MusicaResponse atualizar(Long id, MusicaRequest request) {
        Musica musica = buscarEntidade(id);
        validar(request);
        Genero genero = buscarGenero(request.generoId());

        musica.setTitulo(request.titulo().trim());
        musica.setDuracaoSegundos(request.duracaoSegundos());
        musica.setNumeroFaixa(request.numeroFaixa());
        musica.setAlbumId(request.albumId());
        musica.setGenero(genero);

        return toResponse(musicaRepository.save(musica));
    }

    @Transactional
    public MusicaResponse registrarPlay(Long id, Long usuarioId) {
        Musica musica = buscarEntidade(id);
        historicoService.registrar(usuarioId, musica);
        long atual = musica.getReproducoes() == null ? 0L : musica.getReproducoes();
        musica.setReproducoes(atual + 1);
        return toResponse(musicaRepository.save(musica));
    }

    @Transactional
    public void excluir(Long id) {
        Musica musica = buscarEntidade(id);
        musicaRepository.delete(musica);
    }

    private void validar(MusicaRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();

        if (request.titulo() == null || request.titulo().trim().isEmpty()) {
            campos.put("titulo", "Titulo da musica e obrigatorio.");
        }
        if (request.duracaoSegundos() == null || request.duracaoSegundos() <= 0) {
            campos.put("duracaoSegundos", "Duracao deve ser maior que zero.");
        }
        if (request.numeroFaixa() != null && request.numeroFaixa() <= 0) {
            campos.put("numeroFaixa", "Numero da faixa deve ser maior que zero.");
        }
        if (request.albumId() == null || request.albumId() <= 0) {
            campos.put("albumId", "Album e obrigatorio.");
        }
        if (request.generoId() == null || request.generoId() <= 0) {
            campos.put("generoId", "Genero e obrigatorio.");
        }

        if (!campos.isEmpty()) {
            throw new CatalogValidationException("Dados de musica invalidos.", campos);
        }
    }

    private Genero buscarGenero(Long id) {
        return generoRepository.findById(id)
                .orElseThrow(() -> new CatalogNotFoundException("Genero nao encontrado."));
    }

    private Musica buscarEntidade(Long id) {
        return musicaRepository.findById(id)
                .orElseThrow(() -> new CatalogNotFoundException("Musica nao encontrada."));
    }

    private String limparFiltro(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "";
        }
        return valor.trim();
    }

    private MusicaResponse toResponse(Musica musica) {
        return new MusicaResponse(
                musica.getId(),
                musica.getTitulo(),
                musica.getDuracaoSegundos(),
                musica.getNumeroFaixa(),
                musica.getReproducoes(),
                musica.getAlbumId(),
                musica.getGenero().getId(),
                musica.getGenero().getNome(),
                musica.getCriadoEm()
        );
    }
}
