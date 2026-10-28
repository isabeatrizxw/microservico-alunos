package com.estudante.microservico.dto;

import jakarta.validation.constraints.NotNull;

public record MoverAlunoRequestDTO(

        @NotNull(message = "Informe a nova turma.")
        Long turmaId

) {
}
