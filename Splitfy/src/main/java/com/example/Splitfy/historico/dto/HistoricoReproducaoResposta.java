package com.example.Splitfy.historico.dto;

import java.time.LocalDateTime;

public record HistoricoReproducaoResposta(
        Long id,
        Long usuarioId,
        Long musicaId,
        LocalDateTime reproduzidoEm
) {
}
