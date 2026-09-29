package com.example.Splitfy.catalog.exception;

import com.example.Splitfy.catalog.dto.CatalogErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.Splitfy.catalog")
public class CatalogExceptionHandler {

    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<CatalogErrorResponse> handleNotFound(CatalogNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CatalogValidationException.class)
    public ResponseEntity<CatalogErrorResponse> handleValidation(CatalogValidationException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), exception.getFields());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CatalogErrorResponse> handleDataIntegrity() {
        return build(
                HttpStatus.BAD_REQUEST,
                "Nao foi possivel salvar. Verifique se album e genero existem e se os dados nao estao duplicados.",
                Map.of()
        );
    }

    private ResponseEntity<CatalogErrorResponse> build(
            HttpStatus status,
            String message,
            Map<String, String> fields
    ) {
        CatalogErrorResponse response = new CatalogErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                fields,
                Instant.now()
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraDeNegocio(RegraDeNegocioException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroResponse> handleDadosInvalidos(DadosInvalidosException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErroResponse.CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErroResponse.CampoErro(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Dados de entrada inválidos", request, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleCorpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'", request, List.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        return build(ex.getStatusCode(), ex.getReason(), request, List.of());
    }

    private ResponseEntity<ErroResponse> build(HttpStatusCode status, String mensagem, HttpServletRequest request,
                                               List<ErroResponse.CampoErro> campos) {
        String erro = status instanceof HttpStatus hs ? hs.getReasonPhrase() : String.valueOf(status.value());
        ErroResponse body = new ErroResponse(
                LocalDateTime.now(), status.value(), erro, mensagem, request.getRequestURI(), campos);
        return ResponseEntity.status(status).body(body);
    }
}
