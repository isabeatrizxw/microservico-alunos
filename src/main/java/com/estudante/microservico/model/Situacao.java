package com.estudante.microservico.model;

/** Resultado de uma média em relação à nota de corte (aluno.nota-de-corte). */
public enum Situacao {

    /** Média acima da nota de corte. */
    APROVADO,

    /** Média abaixo ou igual à nota de corte (igual também reprova). */
    REPROVADO,

    /** Ainda não há notas lançadas para calcular a média. */
    SEM_NOTAS

}
