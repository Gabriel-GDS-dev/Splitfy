package com.example.Splitfy.catalog.repository;

import com.example.Splitfy.catalog.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findByNomeContainingIgnoreCase(String nome);

    List<Usuario> findAllByOrderByNomeAsc();

    List<Usuario> findAllByAtivoOrderByNomeAsc(Boolean ativo);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

}