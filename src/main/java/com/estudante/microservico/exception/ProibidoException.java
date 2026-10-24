package com.estudante.microservico.exception;

/**
 * Usuário logado e com o papel certo, mas sem direito sobre este registro específico
 * (ex.: professor alterando nota lançada por outro professor). Vira HTTP 403.
 */
public class ProibidoException extends RuntimeException {

    public ProibidoException(String mensagem) {
        super(mensagem);
    }

}
