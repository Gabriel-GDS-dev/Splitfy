package com.example.Splitfy.catalog.controller;

import com.example.Splitfy.catalog.dto.UsuarioRequest;
import com.example.Splitfy.catalog.dto.UsuarioResponse;
import com.example.Splitfy.catalog.service.PerfilService;
import com.example.Splitfy.catalog.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService, PerfilService perfilService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) Boolean ativo) { return usuarioService.listar(ativo); }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) { return usuarioService.buscar(id); }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody UsuarioRequest request) { return usuarioService.atualizar(id, request); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        usuarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        usuarioService.ativar(id);
        return ResponseEntity.noContent().build();
    }


}
