package com.example.Splitfy.catalog.repository;

import com.example.Splitfy.catalog.entity.Musica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MusicaRepository extends JpaRepository<Musica, Long> {

    @Modifying
    @Query(value = "delete from playlist_musica where musica_id = :id", nativeQuery = true)
    void excluirVinculosPlaylists(@Param("id") Long id);

    @Modifying
    @Query(value = "delete from avaliacao where musica_id = :id", nativeQuery = true)
    void excluirAvaliacoes(@Param("id") Long id);

    @Modifying
    @Query(value = "delete from historico_reproducao where musica_id = :id", nativeQuery = true)
    void excluirHistorico(@Param("id") Long id);

    List<Musica> findByGeneroIdOrderByTituloAsc(Long generoId);

    List<Musica> findByAlbumIdOrderByTituloAsc(Long albumId);

    boolean existsByGeneroId(Long generoId);

    boolean existsByAlbumId(Long albumId);

    @Query("""
            select m from Musica m
            where (:titulo is null or lower(m.titulo) like lower(concat('%', :titulo, '%')))
              and (:generoId is null or m.genero.id = :generoId)
              and (:albumId is null or m.albumId = :albumId)
            order by m.titulo asc
            """)
    List<Musica> search(
            @Param("titulo") String titulo,
            @Param("generoId") Long generoId,
            @Param("albumId") Long albumId
    );
}
