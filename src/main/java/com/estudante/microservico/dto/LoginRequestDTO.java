package com.estudante.microservico.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        String senha

) {
}
