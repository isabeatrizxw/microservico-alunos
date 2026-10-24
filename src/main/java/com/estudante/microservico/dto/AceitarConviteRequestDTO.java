package com.estudante.microservico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ao aceitar o convite a pessoa só escolhe nome e senha; e-mail, papel, escola e turma vêm do convite. */
public record AceitarConviteRequestDTO(

        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome pode ter no máximo 150 caracteres.")
        String nome,

        // Máximo de 72: o BCrypt ignora o que passar disso
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha precisa ter entre 8 e 72 caracteres.")
        String senha

) {
}
