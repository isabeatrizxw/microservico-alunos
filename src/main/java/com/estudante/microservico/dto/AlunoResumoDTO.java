package com.estudante.microservico.dto;

import com.estudante.microservico.model.Usuario;

/** Aluno de uma turma, como o professor vê na lista para lançar notas. */
public record AlunoResumoDTO(Long id, String nome, String email) {

    public static AlunoResumoDTO de(Usuario aluno) {
        return new AlunoResumoDTO(aluno.getId(), aluno.getNome(), aluno.getEmail());
    }

}
