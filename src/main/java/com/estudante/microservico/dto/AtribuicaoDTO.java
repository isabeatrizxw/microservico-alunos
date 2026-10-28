package com.estudante.microservico.dto;

import com.estudante.microservico.model.Atribuicao;

/** Aula atribuída: o professor dá esta disciplina nesta turma. */
public record AtribuicaoDTO(
        Long id,
        Long professorId,
        String professorNome,
        Long turmaId,
        String turmaNome,
        Long disciplinaId,
        String disciplinaNome
) {

    public static AtribuicaoDTO de(Atribuicao atribuicao) {
        return new AtribuicaoDTO(atribuicao.getId(),
                atribuicao.getProfessor().getId(), atribuicao.getProfessor().getNome(),
                atribuicao.getTurma().getId(), atribuicao.getTurma().getNome(),
                atribuicao.getDisciplina().getId(), atribuicao.getDisciplina().getNome());
    }

}
