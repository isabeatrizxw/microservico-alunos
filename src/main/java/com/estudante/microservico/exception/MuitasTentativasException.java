package com.estudante.microservico.exception;

/** Limite de tentativas atingido (login ou cadastro). Vira HTTP 429. */
public class MuitasTentativasException extends RuntimeException {

    public MuitasTentativasException() {
        super("Muitas tentativas. Aguarde alguns minutos e tente de novo.");
    }

}
