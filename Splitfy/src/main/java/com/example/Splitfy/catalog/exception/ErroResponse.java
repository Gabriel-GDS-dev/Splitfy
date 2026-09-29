package com.example.Splitfy.catalog.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoErro> errosDeCampo
) {

    public record CampoErro(String campo, String mensagem) {
    }
}
