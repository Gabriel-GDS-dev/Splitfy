package com.example.Splitfy.catalog.service;

import com.example.Splitfy.catalog.dto.UsuarioRequest;
import com.example.Splitfy.catalog.dto.UsuarioResponse;
import com.example.Splitfy.catalog.entity.Usuario;
import com.example.Splitfy.catalog.exception.UsuarioNotFoundException;
import com.example.Splitfy.catalog.exception.UsuarioValidationException;
import com.example.Splitfy.catalog.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Boolean ativo) {
        List<Usuario> usuarios = ativo == null
                ? usuarioRepository.findAllByOrderByNomeAsc()
                : usuarioRepository.findAllByAtivoOrderByNomeAsc(ativo);
        return usuarios.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        validar(request, null, true);

        String email = normalizarEmail(request.email());
        String role = normalizarRole(request.role());
        if (!administradorAutenticado()) {
            if ("ADMIN".equals(role)) {
                throw new AccessDeniedException("Apenas administradores podem atribuir a role ADMIN.");
            }
            role = "USER";
        }

        Usuario usuario = new Usuario(
                limparObrigatorio(request.nome()),
                email,
                passwordEncoder.encode(request.senha()),
                parseData(request.dataNascimento()),
                role
        );

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscarEntidade(id);
        validar(request, id, false);

        usuario.setNome(limparObrigatorio(request.nome()));
        usuario.setEmail(normalizarEmail(request.email()));
        usuario.setDataNascimento(parseData(request.dataNascimento()));
        usuario.setRole(normalizarRole(request.role()));

        if (request.senha() != null && !request.senha().trim().isEmpty()) {
            usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void desativar(Long id) {
        Usuario usuario = buscarEntidade(id);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void ativar(Long id) {
        Usuario usuario = buscarEntidade(id);
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException("Usuario nao encontrado."));
    }

    private void validar(UsuarioRequest request, Long id, boolean senhaObrigatoria) {
        Map<String, String> campos = new LinkedHashMap<>();

        if (request == null) {
            throw new UsuarioValidationException(
                    "Dados de usuario invalidos.",
                    Map.of("usuario", "Corpo da requisicao e obrigatorio.")
            );
        }

        if (request.nome() == null || request.nome().trim().isEmpty()) {
            campos.put("nome", "Nome e obrigatorio.");
        }

        String email = normalizarEmail(request.email());

        if (email == null) {
            campos.put("email", "Email e obrigatorio.");
        } else if ((id == null && usuarioRepository.existsByEmailIgnoreCase(email)) ||
                (id != null && usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id))) {
            campos.put("email", "Ja existe um usuario com esse email.");
        }

        if (senhaObrigatoria &&
                (request.senha() == null || request.senha().trim().length() < 6)) {
            campos.put("senha", "Senha deve possuir pelo menos 6 caracteres.");
        } else if (!senhaObrigatoria && request.senha() != null &&
                !request.senha().trim().isEmpty() && request.senha().trim().length() < 6) {
            campos.put("senha", "Senha deve possuir pelo menos 6 caracteres.");
        }

        if (request.dataNascimento() == null ||
                request.dataNascimento().trim().isEmpty()) {

            campos.put("dataNascimento", "Data de nascimento e obrigatoria.");

        } else {
            try {
                parseData(request.dataNascimento());
            } catch (UsuarioValidationException e) {
                campos.putAll(e.getFields());
            }
        }

        String role = normalizarRole(request.role());

        if (role == null) {
            campos.put("role", "Role deve ser USER ou ADMIN.");
        }

        if (!campos.isEmpty()) {
            throw new UsuarioValidationException(
                    "Dados de usuario invalidos.",
                    campos
            );
        }
    }

    private LocalDate parseData(String valor) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new UsuarioValidationException(
                    "Dados de usuario invalidos.",
                    Map.of(
                            "dataNascimento",
                            "Use o formato AAAA-MM-DD."
                    )
            );
        }
    }

    private String normalizarEmail(String valor) {
        if (valor == null ||
                valor.trim().isEmpty() ||
                !valor.contains("@")) {
            return null;
        }

        return valor.trim().toLowerCase();
    }

    private String normalizarRole(String valor) {
        String role = valor == null ||
                valor.trim().isEmpty()
                ? "USER"
                : valor.trim().toUpperCase();

        return role.equals("USER") || role.equals("ADMIN")
                ? role
                : null;
    }

    private String limparObrigatorio(String valor) {
        return valor.trim();
    }

    private boolean administradorAutenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.getAuthorities().stream()
                .anyMatch(permissao -> "ROLE_ADMIN".equals(permissao.getAuthority()));
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getDataNascimento(),
                usuario.getRole(),
                usuario.isAtivo(),
                usuario.getCriadoEm()
        );
    }
}
