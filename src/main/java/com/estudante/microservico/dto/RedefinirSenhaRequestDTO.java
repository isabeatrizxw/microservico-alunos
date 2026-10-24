package com.estudante.microservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Código do link (vai no corpo, não na URL, para não ficar em logs de acesso) e a senha nova. */
public record RedefinirSenhaRequestDTO(

        @NotBlank(message = "O código do link é obrigatório.")
        @Size(max = 100, message = "Código inválido.")
        String codigo,

        // Máximo de 72: o BCrypt ignora o que passar disso
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha precisa ter entre 8 e 72 caracteres.")
        String senha

) {
}
