package com.example.Splitfy.historico.repository;

import com.example.Splitfy.historico.entity.HistoricoReproducao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistoricoReproducaoRepository extends JpaRepository<HistoricoReproducao, Long> {

    @Query("""
            select h from HistoricoReproducao h
            where h.usuarioId = :usuarioId
            order by h.reproduzidoEm desc, h.id desc
            """)
    List<HistoricoReproducao> listarPorUsuario(@Param("usuarioId") Long usuarioId, Pageable limite);

    @Query(value = "select exists(select 1 from usuario where id = :usuarioId)", nativeQuery = true)
    boolean usuarioExiste(@Param("usuarioId") Long usuarioId);
}
