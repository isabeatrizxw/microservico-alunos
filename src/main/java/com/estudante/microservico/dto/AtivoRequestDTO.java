package com.estudante.microservico.dto;

import jakarta.validation.constraints.NotNull;

/** Ativar (true) ou desativar (false) o acesso de um aluno ou professor. */
public record AtivoRequestDTO(

        @NotNull(message = "Informe se a conta fica ativa.")
        Boolean ativo

) {
}
