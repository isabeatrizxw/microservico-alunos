package com.estudante.microservico.dto;

import com.estudante.microservico.model.Situacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Notas de um aluno agrupadas por disciplina, com a média e a situação de cada disciplina. */
public record BoletimDTO(
        Long alunoId,
        String alunoNome,
        String turmaNome,
        List<PorDisciplina> disciplinas,
        LocalDateTime calculadoEm
) {

    public record PorDisciplina(
            Long disciplinaId,
            String disciplinaNome,
            List<NotaResponseDTO> notas,
            BigDecimal media,
            Situacao situacao
    ) {
    }

}
