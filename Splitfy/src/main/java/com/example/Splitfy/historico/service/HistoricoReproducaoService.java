package com.example.Splitfy.historico.service;

import com.example.Splitfy.catalog.entity.Musica;
import com.example.Splitfy.historico.dto.HistoricoReproducaoResposta;
import com.example.Splitfy.historico.entity.HistoricoReproducao;
import com.example.Splitfy.historico.repository.HistoricoReproducaoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class HistoricoReproducaoService {

    private final HistoricoReproducaoRepository historicoRepository;

    public HistoricoReproducaoService(HistoricoReproducaoRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    @Transactional
    public void registrar(Long usuarioId, Musica musica) {
        validarUsuario(usuarioId);
        historicoRepository.save(new HistoricoReproducao(usuarioId, musica));
    }

    @Transactional(readOnly = true)
    public List<HistoricoReproducaoResposta> consultar(Long usuarioId, int limite) {
        if (limite < 1 || limite > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O limite deve estar entre 1 e 100.");
        }
        validarUsuario(usuarioId);
        return historicoRepository.listarPorUsuario(usuarioId, PageRequest.of(0, limite)).stream()
                .map(historico -> new HistoricoReproducaoResposta(
                        historico.getId(),
                        historico.getUsuarioId(),
                        historico.getMusica().getId(),
                        historico.getReproduzidoEm()
                ))
                .toList();
    }

    private void validarUsuario(Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um ID de usuário positivo.");
        }
        if (!historicoRepository.usuarioExiste(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
        }
    }
}
