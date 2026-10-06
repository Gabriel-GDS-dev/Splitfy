package com.example.Splitfy.seguranca;

public record TokenResposta(String tokenAcesso, String tipo, long expiraEmSegundos) {
}
