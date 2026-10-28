package com.estudante.microservico.dto;

import com.estudante.microservico.model.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Convite feito pelo ADMIN_ESCOLA. A escola não vem no pedido (é sempre a de quem está logado).
 * {@code turmaId} é obrigatório para ALUNO e proibido para PROFESSOR.
 */
public record NovoConviteRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "O e-mail pode ter no máximo 254 caracteres.")
        String email,

        @NotNull(message = "O papel é obrigatório (PROFESSOR ou ALUNO).")
        Papel papel,

        Long turmaId

) {
}
