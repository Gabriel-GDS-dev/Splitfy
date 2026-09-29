package com.example.Splitfy.catalog.service;

import com.example.Splitfy.catalog.dto.ArtistaMapper;
import com.example.Splitfy.catalog.dto.ArtistaRequest;
import com.example.Splitfy.catalog.dto.ArtistaResponse;
import com.example.Splitfy.catalog.entity.Artista;
import com.example.Splitfy.catalog.exception.RecursoNaoEncontradoException;
import com.example.Splitfy.catalog.exception.RegraDeNegocioException;
import com.example.Splitfy.catalog.repository.AlbumRepository;
import com.example.Splitfy.catalog.repository.ArtistaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ArtistaService {

    private final ArtistaRepository artistaRepository;
    private final AlbumRepository albumRepository;

    public ArtistaService(ArtistaRepository artistaRepository, AlbumRepository albumRepository) {
        this.artistaRepository = artistaRepository;
        this.albumRepository = albumRepository;
    }

    @Transactional
    public ArtistaResponse criar(ArtistaRequest request) {
        Artista artista = artistaRepository.save(ArtistaMapper.toEntity(request));
        return ArtistaMapper.toResponse(artista);
    }

    @Transactional(readOnly = true)
    public Page<ArtistaResponse> listar(String nome, Pageable pageable) {
        Page<Artista> pagina = StringUtils.hasText(nome)
                ? artistaRepository.findByNomeContainingIgnoreCase(nome.trim(), pageable)
                : artistaRepository.findAll(pageable);
        return pagina.map(ArtistaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ArtistaResponse buscarPorId(Long id) {
        return ArtistaMapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public ArtistaResponse atualizar(Long id, ArtistaRequest request) {
        Artista artista = buscarEntidade(id);
        ArtistaMapper.updateEntity(artista, request);
        return ArtistaMapper.toResponse(artista);
    }

    @Transactional
    public void excluir(Long id) {
        Artista artista = buscarEntidade(id);
        if (albumRepository.existsByArtistaId(id)) {
            throw new RegraDeNegocioException(
                    "Não é possível excluir o artista " + id + " porque ele possui álbuns cadastrados. Exclua os álbuns antes.");
        }
        artistaRepository.delete(artista);
    }

    private Artista buscarEntidade(Long id) {
        return artistaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Artista", id));
    }
}
