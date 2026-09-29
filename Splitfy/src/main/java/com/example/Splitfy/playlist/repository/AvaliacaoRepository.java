package com.example.Splitfy.playlist.repository;

import com.example.Splitfy.playlist.entity.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    @Query("select a from Avaliacao a where a.usuarioId = :usuarioId and a.musica.id = :musicaId")
    Optional<Avaliacao> findByUsuarioIdAndMusicaId(@Param("usuarioId") Long usuarioId, @Param("musicaId") Long musicaId);

    @Query("select case when count(a) > 0 then true else false end from Avaliacao a where a.usuarioId = :usuarioId and a.musica.id = :musicaId")
    boolean existsByUsuarioIdAndMusicaId(@Param("usuarioId") Long usuarioId, @Param("musicaId") Long musicaId);

    @Query("select count(a) from Avaliacao a where a.musica.id = :musicaId and a.curtido = true")
    long contarCurtidas(@Param("musicaId") Long musicaId);

    @Query(value = "select exists(select 1 from usuario where id = :usuarioId)", nativeQuery = true)
    boolean usuarioExiste(@Param("usuarioId") Long usuarioId);
}
