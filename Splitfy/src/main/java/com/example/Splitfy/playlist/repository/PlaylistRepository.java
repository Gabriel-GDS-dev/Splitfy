package com.example.Splitfy.playlist.repository;

import com.example.Splitfy.playlist.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    List<Playlist> findByUsuarioIdOrderByNomeAsc(Long usuarioId);

    @Query(value = "select exists(select 1 from usuario where id = :usuarioId)", nativeQuery = true)
    boolean usuarioExiste(@Param("usuarioId") Long usuarioId);
}
