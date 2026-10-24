package com.estudante.microservico.exception;

/** Pedido com combinação de dados que a regra de negócio não aceita (ex.: aluno sem turma). Vira HTTP 400. */
public class PedidoInvalidoException extends RuntimeException {

    public PedidoInvalidoException(String mensagem) {
        super(mensagem);
    }

}
