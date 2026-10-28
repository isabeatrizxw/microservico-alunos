package com.estudante.microservico.dto;

import jakarta.validation.constraints.NotNull;

/** Atribui ao professor a disciplina numa turma. */
public record AtribuicaoRequestDTO(

        @NotNull(message = "Escolha o professor.")
        Long professorId,

        @NotNull(message = "Escolha a turma.")
        Long turmaId,

        @NotNull(message = "Escolha a disciplina.")
        Long disciplinaId

) {
}
