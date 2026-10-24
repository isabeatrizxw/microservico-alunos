package com.estudante.microservico.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cadastro público de professor autônomo. {@code nomeEspaco} é opcional (padrão: "Aulas de {nome}"). */
public record CadastroAutonomoRequestDTO(

        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome pode ter no máximo 150 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "O e-mail pode ter no máximo 254 caracteres.")
        String email,

        // Máximo de 72: o BCrypt ignora o que passar disso
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha precisa ter entre 8 e 72 caracteres.")
        String senha,

        @Size(max = 150, message = "O nome do espaço pode ter no máximo 150 caracteres.")
        String nomeEspaco

) {
}
