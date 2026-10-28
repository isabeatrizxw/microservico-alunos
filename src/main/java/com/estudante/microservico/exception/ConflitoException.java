package com.estudante.microservico.exception;

/** Pedido válido, mas que conflita com dados já salvos (ex.: e-mail já cadastrado). Vira HTTP 409. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }

}
