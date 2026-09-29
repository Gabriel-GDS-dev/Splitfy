package com.example.Splitfy.playlist.repository;

import com.example.Splitfy.playlist.entity.PlaylistMusica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaylistMusicaRepository extends JpaRepository<PlaylistMusica, Long> {

    @Query("select pm from PlaylistMusica pm where pm.playlist.id = :playlistId order by pm.adicionadoEm asc")
    List<PlaylistMusica> findByPlaylistId(@Param("playlistId") Long playlistId);

    @Query("select case when count(pm) > 0 then true else false end from PlaylistMusica pm where pm.playlist.id = :playlistId and pm.musica.id = :musicaId")
    boolean existsByPlaylistIdAndMusicaId(@Param("playlistId") Long playlistId, @Param("musicaId") Long musicaId);

    @Query("select pm from PlaylistMusica pm where pm.playlist.id = :playlistId and pm.musica.id = :musicaId")
    Optional<PlaylistMusica> findByPlaylistIdAndMusicaId(@Param("playlistId") Long playlistId, @Param("musicaId") Long musicaId);
}
