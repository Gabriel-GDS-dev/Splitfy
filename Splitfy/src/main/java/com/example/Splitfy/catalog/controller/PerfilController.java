package com.example.Splitfy.catalog.controller;

import com.example.Splitfy.catalog.dto.PerfilRequest;
import com.example.Splitfy.catalog.dto.PerfilResponse;
import com.example.Splitfy.catalog.service.PerfilService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios/{usuarioId}/perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public PerfilResponse buscarPerfil(@PathVariable Long usuarioId) {
        return perfilService.buscarPorUsuario(usuarioId);
    }

    @PostMapping
    public ResponseEntity<PerfilResponse> criarPerfil(
            @PathVariable Long usuarioId,
            @RequestBody PerfilRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(perfilService.criar(usuarioId, request));
    }

    @PutMapping
    public PerfilResponse atualizarPerfil(
            @PathVariable Long usuarioId,
            @RequestBody PerfilRequest request) {

        return perfilService.atualizar(usuarioId, request);
    }

    @DeleteMapping
    public ResponseEntity<Void> excluirPerfil(
            @PathVariable Long usuarioId) {

        perfilService.excluir(usuarioId);

        return ResponseEntity.noContent().build();
    }
}