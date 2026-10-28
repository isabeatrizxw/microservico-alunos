package com.estudante.microservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A instituição não vem no pedido: é sempre a de quem está logado. */
public record DisciplinaRequestDTO(

        @NotBlank(message = "O nome da disciplina é obrigatório.")
        @Size(max = 100, message = "O nome da disciplina pode ter no máximo 100 caracteres.")
        String nome

) {
}
