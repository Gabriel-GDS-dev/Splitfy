package com.example.Splitfy.catalog.repository;

import com.example.Splitfy.catalog.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Optional<Perfil> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioId(Long usuarioId);
}
