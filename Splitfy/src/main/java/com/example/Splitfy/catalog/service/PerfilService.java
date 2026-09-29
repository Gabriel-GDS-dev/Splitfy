package com.example.Splitfy.catalog.service;

import com.example.Splitfy.catalog.dto.PerfilRequest;
import com.example.Splitfy.catalog.dto.PerfilResponse;
import com.example.Splitfy.catalog.entity.Perfil;
import com.example.Splitfy.catalog.entity.Usuario;
import com.example.Splitfy.catalog.exception.UsuarioNotFoundException;
import com.example.Splitfy.catalog.exception.UsuarioValidationException;
import com.example.Splitfy.catalog.repository.PerfilRepository;
import com.example.Splitfy.catalog.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PerfilService {
    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;

    public PerfilService(PerfilRepository perfilRepository, UsuarioRepository usuarioRepository) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PerfilResponse buscarPorUsuario(Long usuarioId) {
        validarUsuario(usuarioId);
        Perfil perfil = perfilRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new UsuarioNotFoundException("Perfil nao encontrado."));
        return toResponse(perfil);
    }

    @Transactional
    public PerfilResponse criar(Long usuarioId, PerfilRequest request) {
        Usuario usuario = validarUsuario(usuarioId);
        validar(request);
        if (perfilRepository.existsByUsuarioId(usuarioId)) {
            throw new UsuarioValidationException("Dados de perfil invalidos.", Map.of("usuarioId", "O usuario ja possui um perfil."));
        }
        Perfil perfil = new Perfil(usuario, request.nomeExibicao().trim(), limpar(request.bio()), limpar(request.fotoUrl()));
        return toResponse(perfilRepository.save(perfil));
    }

    @Transactional
    public PerfilResponse atualizar(Long usuarioId, PerfilRequest request) {
        validarUsuario(usuarioId);
        validar(request);
        Perfil perfil = perfilRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new UsuarioNotFoundException("Perfil nao encontrado."));
        perfil.setNomeExibicao(request.nomeExibicao().trim());
        perfil.setBio(limpar(request.bio()));
        perfil.setFotoUrl(limpar(request.fotoUrl()));
        return toResponse(perfilRepository.save(perfil));
    }

    @Transactional
    public void excluir(Long usuarioId) {
        validarUsuario(usuarioId);
        Perfil perfil = perfilRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new UsuarioNotFoundException("Perfil nao encontrado."));
        perfilRepository.delete(perfil);
    }

    private Usuario validarUsuario(Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0) throw new UsuarioValidationException("Dados invalidos.", Map.of("usuarioId", "Informe um ID positivo."));
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNotFoundException("Usuario nao encontrado."));
    }

    private void validar(PerfilRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        if (request == null) campos.put("perfil", "Corpo da requisicao e obrigatorio.");
        else if (request.nomeExibicao() == null || request.nomeExibicao().trim().isEmpty()) campos.put("nomeExibicao", "Nome de exibicao e obrigatorio.");
        if (!campos.isEmpty()) throw new UsuarioValidationException("Dados de perfil invalidos.", campos);
    }

    private String limpar(String valor) { return valor == null || valor.trim().isEmpty() ? null : valor.trim(); }

    private PerfilResponse toResponse(Perfil perfil) {
        return new PerfilResponse(perfil.getId(), perfil.getUsuario().getId(), perfil.getNomeExibicao(), perfil.getBio(), perfil.getFotoUrl(), perfil.getCriadoEm());
    }
}
