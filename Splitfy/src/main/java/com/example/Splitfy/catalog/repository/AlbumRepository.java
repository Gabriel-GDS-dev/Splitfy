package com.example.Splitfy.catalog.repository;

import com.example.Splitfy.catalog.entity.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// As consultas carregam o artista junto (EntityGraph) porque a resposta sempre expõe artistaNome.
public interface AlbumRepository extends JpaRepository<Album, Long> {

    @EntityGraph(attributePaths = "artista")
    @Query("select a from Album a where a.artista.id = :artistaId and lower(trim(a.titulo)) = lower(trim(:titulo)) order by a.id")
    java.util.List<Album> buscarExistentes(@Param("artistaId") Long artistaId, @Param("titulo") String titulo);

    @Override
    @EntityGraph(attributePaths = "artista")
    Optional<Album> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "artista")
    Page<Album> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "artista")
    Page<Album> findByArtistaId(Long artistaId, Pageable pageable);

    @EntityGraph(attributePaths = "artista")
    Page<Album> findByAnoLancamento(Integer anoLancamento, Pageable pageable);

    @EntityGraph(attributePaths = "artista")
    Page<Album> findByArtistaIdAndAnoLancamento(Long artistaId, Integer anoLancamento, Pageable pageable);

    boolean existsByArtistaId(Long artistaId);
}
