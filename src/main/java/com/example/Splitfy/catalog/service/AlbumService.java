package com.example.Splitfy.catalog.service;

import com.example.Splitfy.catalog.dto.AlbumMapper;
import com.example.Splitfy.catalog.dto.AlbumRequest;
import com.example.Splitfy.catalog.dto.AlbumResponse;
import com.example.Splitfy.catalog.entity.Album;
import com.example.Splitfy.catalog.entity.Artista;
import com.example.Splitfy.catalog.exception.DadosInvalidosException;
import com.example.Splitfy.catalog.exception.RecursoNaoEncontradoException;
import com.example.Splitfy.catalog.repository.AlbumRepository;
import com.example.Splitfy.catalog.repository.ArtistaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final ArtistaRepository artistaRepository;

    public AlbumService(AlbumRepository albumRepository, ArtistaRepository artistaRepository) {
        this.albumRepository = albumRepository;
        this.artistaRepository = artistaRepository;
    }

    @Transactional
    public AlbumResponse criar(AlbumRequest request) {
        validarAno(request.anoLancamento());
        Artista artista = buscarArtista(request.artistaId());
        Album album = albumRepository.save(AlbumMapper.toEntity(request, artista));
        return AlbumMapper.toResponse(album);
    }

    @Transactional(readOnly = true)
    public Page<AlbumResponse> listar(Integer ano, Long artistaId, Pageable pageable) {
        Page<Album> pagina;
        if (ano != null && artistaId != null) {
            pagina = albumRepository.findByArtistaIdAndAnoLancamento(artistaId, ano, pageable);
        } else if (artistaId != null) {
            pagina = albumRepository.findByArtistaId(artistaId, pageable);
        } else if (ano != null) {
            pagina = albumRepository.findByAnoLancamento(ano, pageable);
        } else {
            pagina = albumRepository.findAll(pageable);
        }
        return pagina.map(AlbumMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AlbumResponse> listarPorArtista(Long artistaId, Pageable pageable) {
        if (!artistaRepository.existsById(artistaId)) {
            throw new RecursoNaoEncontradoException("Artista", artistaId);
        }
        return albumRepository.findByArtistaId(artistaId, pageable).map(AlbumMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AlbumResponse buscarPorId(Long id) {
        return AlbumMapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public AlbumResponse atualizar(Long id, AlbumRequest request) {
        validarAno(request.anoLancamento());
        Album album = buscarEntidade(id);
        Artista artista = album.getArtista().getId().equals(request.artistaId())
                ? album.getArtista()
                : buscarArtista(request.artistaId());
        AlbumMapper.updateEntity(album, request, artista);
        return AlbumMapper.toResponse(album);
    }

    @Transactional
    public void excluir(Long id) {
        Album album = buscarEntidade(id);
        // TODO: quando a entidade Musica (módulo do Gabriel) existir, bloquear com RegraDeNegocioException (409)
        //  se o álbum tiver músicas, ex.: if (musicaRepository.existsByAlbumId(id)) throw ...
        albumRepository.delete(album);
    }

    private Album buscarEntidade(Long id) {
        return albumRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Álbum", id));
    }

    private Artista buscarArtista(Long artistaId) {
        return artistaRepository.findById(artistaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Artista", artistaId));
    }

    private void validarAno(Integer ano) {
        int anoAtual = Year.now().getValue();
        if (ano != null && ano > anoAtual) {
            throw new DadosInvalidosException(
                    "anoLancamento não pode ser maior que o ano atual (" + anoAtual + ")");
        }
    }
}
