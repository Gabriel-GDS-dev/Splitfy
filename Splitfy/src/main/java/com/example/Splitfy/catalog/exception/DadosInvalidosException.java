package com.example.Splitfy.catalog.exception;

// Validação de entrada que depende de regra calculada no service (ex.: ano não pode ser futuro).
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
