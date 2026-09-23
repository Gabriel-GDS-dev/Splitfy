package com.example.Splitfy.catalog.service;

import com.example.Splitfy.catalog.dto.GeneroRequest;
import com.example.Splitfy.catalog.dto.GeneroResponse;
import com.example.Splitfy.catalog.entity.Genero;
import com.example.Splitfy.catalog.exception.CatalogNotFoundException;
import com.example.Splitfy.catalog.exception.CatalogValidationException;
import com.example.Splitfy.catalog.repository.GeneroRepository;
import com.example.Splitfy.catalog.repository.MusicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeneroService {

    private final GeneroRepository generoRepository;
    private final MusicaRepository musicaRepository;

    public GeneroService(GeneroRepository generoRepository, MusicaRepository musicaRepository) {
        this.generoRepository = generoRepository;
        this.musicaRepository = musicaRepository;
    }

    @Transactional(readOnly = true)
    public List<GeneroResponse> listar() {
        return generoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GeneroResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public GeneroResponse criar(GeneroRequest request) {
        String nome = validarNome(request.nome(), null);
        Genero genero = new Genero(nome, limparTexto(request.descricao()));
        return toResponse(generoRepository.save(genero));
    }

    @Transactional
    public GeneroResponse atualizar(Long id, GeneroRequest request) {
        Genero genero = buscarEntidade(id);
        String nome = validarNome(request.nome(), id);
        genero.setNome(nome);
        genero.setDescricao(limparTexto(request.descricao()));
        return toResponse(generoRepository.save(genero));
    }

    @Transactional
    public void excluir(Long id) {
        Genero genero = buscarEntidade(id);
        if (musicaRepository.existsByGeneroId(id)) {
            throw new CatalogValidationException(
                    "Genero possui musicas vinculadas.",
                    Map.of("generoId", "Remova ou altere as musicas antes de excluir o genero.")
            );
        }
        generoRepository.delete(genero);
    }

    private Genero buscarEntidade(Long id) {
        return generoRepository.findById(id)
                .orElseThrow(() -> new CatalogNotFoundException("Genero nao encontrado."));
    }

    private String validarNome(String valor, Long idAtual) {
        Map<String, String> campos = new LinkedHashMap<>();
        String nome = limparTexto(valor);
        if (nome == null) {
            campos.put("nome", "Nome do genero e obrigatorio.");
        } else if ((idAtual == null && generoRepository.existsByNomeIgnoreCase(nome))
                || (idAtual != null && generoRepository.existsByNomeIgnoreCaseAndIdNot(nome, idAtual))) {
            campos.put("nome", "Ja existe um genero com esse nome.");
        }

        if (!campos.isEmpty()) {
            throw new CatalogValidationException("Dados de genero invalidos.", campos);
        }
        return nome;
    }

    private String limparTexto(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        return valor.trim();
    }

    private GeneroResponse toResponse(Genero genero) {
        return new GeneroResponse(genero.getId(), genero.getNome(), genero.getDescricao());
    }
}
