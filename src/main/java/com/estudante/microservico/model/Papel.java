package com.estudante.microservico.model;

/**
 * Papel de um usuário no sistema. O papel nunca é escolhido pela própria pessoa:
 * o SUPER_ADMIN cria as escolas e o ADMIN_ESCOLA define professores e alunos.
 */
public enum Papel {

    /** Dono do sistema: cadastra instituições. Não pertence a nenhuma escola. */
    SUPER_ADMIN,

    /** Secretaria/coordenação de uma escola: cadastra turmas, professores e alunos. */
    ADMIN_ESCOLA,

    /** Vê e lança notas dos alunos da sua instituição. */
    PROFESSOR,

    /**
     * Dá aulas por conta própria: cria a própria conta e é, ao mesmo tempo, administrador e professor
     * da sua "escola de uma pessoa só" (instituição do tipo AUTONOMO). Convida apenas alunos.
     */
    PROFESSOR_AUTONOMO,

    /** Vê apenas as próprias notas e calcula a própria média. */
    ALUNO

}
