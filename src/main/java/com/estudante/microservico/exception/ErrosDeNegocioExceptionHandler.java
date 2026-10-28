package com.estudante.microservico.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Converte erros de regra de negócio e de login em JSON {@code {"erro": mensagem}}.
 * Erros de validação dos DTOs continuam em {@link ValidacaoExceptionHandler}.
 */
@RestControllerAdvice
public class ErrosDeNegocioExceptionHandler {

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<Map<String, String>> tratarConflito(ConflitoException ex) {
        return erro(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> tratarNaoEncontrado(NaoEncontradoException ex) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProibidoException.class)
    public ResponseEntity<Map<String, String>> tratarProibido(ProibidoException ex) {
        return erro(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(PedidoInvalidoException.class)
    public ResponseEntity<Map<String, String>> tratarPedidoInvalido(PedidoInvalidoException ex) {
        return erro(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Dois pedidos ao mesmo tempo podem passar pela checagem e esbarrar na constraint UNIQUE do banco. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> tratarViolacaoDoBanco(DataIntegrityViolationException ex) {
        return erro(HttpStatus.CONFLICT, "Os dados conflitam com um registro já existente.");
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<Map<String, String>> tratarMuitasTentativas(MuitasTentativasException ex) {
        return erro(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    /**
     * Conta inativa: cadastro ainda não confirmado ou desativada pela escola.
     * Só chega aqui com a senha certa (ver SegurancaConfig).
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> tratarContaInativa(DisabledException ex) {
        return erro(HttpStatus.UNAUTHORIZED, MENSAGEM_CONTA_INATIVA);
    }

    static final String MENSAGEM_CONTA_INATIVA = "Sua conta está inativa. Se você acabou de se cadastrar, "
            + "abra o link de confirmação que enviamos por e-mail; se não, fale com a administração da escola.";

    /** Mesma mensagem para e-mail inexistente e senha errada, para não revelar quem tem cadastro. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> tratarFalhaDeLogin(AuthenticationException ex) {
        return erro(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
    }

    private static ResponseEntity<Map<String, String>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }

}
