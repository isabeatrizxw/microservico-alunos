package com.estudante.microservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Código do link de confirmação. Vai no corpo (não na URL) para não ficar em logs de acesso. */
public record ConfirmarEmailRequestDTO(

        @NotBlank(message = "O código de confirmação é obrigatório.")
        @Size(max = 100, message = "Código de confirmação inválido.")
        String codigo

) {
}
