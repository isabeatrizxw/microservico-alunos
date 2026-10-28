package com.estudante.microservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A instituição não vem no pedido: é sempre a de quem está logado. */
public record TurmaRequestDTO(

        @NotBlank(message = "O nome da turma é obrigatório.")
        @Size(max = 100, message = "O nome da turma pode ter no máximo 100 caracteres.")
        String nome

) {
}
