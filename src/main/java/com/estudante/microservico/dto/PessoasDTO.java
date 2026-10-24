package com.estudante.microservico.dto;

import java.util.List;

/** Alunos e professores da escola, como a administração vê. */
public record PessoasDTO(List<Aluno> alunos, List<Professor> professores) {

    public record Aluno(Long id, String nome, String email, Long turmaId, String turmaNome, boolean ativo) {
    }

    public record Professor(Long id, String nome, String email, boolean ativo, List<AtribuicaoDTO> atribuicoes) {
    }

}
