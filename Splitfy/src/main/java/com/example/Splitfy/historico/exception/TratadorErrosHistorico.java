package com.example.Splitfy.historico.exception;

import com.example.Splitfy.catalog.controller.MusicaController;
import com.example.Splitfy.historico.controller.HistoricoReproducaoController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {HistoricoReproducaoController.class, MusicaController.class})
public class TratadorErrosHistorico {

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail tratarErro(ResponseStatusException excecao) {
        ProblemDetail resposta = ProblemDetail.forStatusAndDetail(excecao.getStatusCode(), excecao.getReason());
        resposta.setTitle(excecao.getStatusCode().value() == HttpStatus.NOT_FOUND.value()
                ? "Recurso não encontrado" : "Dados inválidos");
        return resposta;
    }
}
