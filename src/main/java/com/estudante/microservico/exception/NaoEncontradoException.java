package com.estudante.microservico.exception;

/**
 * Registro inexistente ou de outra escola. Vira HTTP 404: para quem não tem acesso,
 * dado de outra escola é indistinguível de dado que não existe.
 */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }

}
