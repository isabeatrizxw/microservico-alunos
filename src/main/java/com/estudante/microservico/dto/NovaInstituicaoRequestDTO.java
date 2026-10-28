package com.estudante.microservico.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cadastro de uma escola nova junto com a conta do seu primeiro administrador. */
public record NovaInstituicaoRequestDTO(

        @NotBlank(message = "O nome da instituição é obrigatório.")
        @Size(max = 150, message = "O nome da instituição pode ter no máximo 150 caracteres.")
        String nome,

        @NotBlank(message = "O nome do administrador é obrigatório.")
        @Size(max = 150, message = "O nome do administrador pode ter no máximo 150 caracteres.")
        String adminNome,

        @NotBlank(message = "O e-mail do administrador é obrigatório.")
        @Email(message = "E-mail do administrador inválido.")
        @Size(max = 254, message = "O e-mail pode ter no máximo 254 caracteres.")
        String adminEmail,

        // Máximo de 72: o BCrypt ignora o que passar disso
        @NotBlank(message = "A senha do administrador é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha precisa ter entre 8 e 72 caracteres.")
        String adminSenha

) {
}
