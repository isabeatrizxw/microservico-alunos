package com.estudante.microservico.dto;

import com.estudante.microservico.model.Situacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * "Média da sala": média de cada aluno da turma numa disciplina e a média da turma
 * (média das médias, sem contar quem ainda não tem notas).
 */
public record MediasTurmaDTO(
        Long turmaId,
        String turmaNome,
        Long disciplinaId,
        String disciplinaNome,
        List<MediaAluno> alunos,
        BigDecimal mediaTurma,
        int aprovados,
        int reprovados,
        int semNotas,
        LocalDateTime calculadoEm
) {

    public record MediaAluno(
            Long alunoId,
            String alunoNome,
            int quantidadeNotas,
            BigDecimal media,
            Situacao situacao
    ) {
    }

}
