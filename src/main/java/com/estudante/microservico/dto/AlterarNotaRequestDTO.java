package com.estudante.microservico.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Correção de uma nota. Aluno e disciplina não mudam: para isso, apague e lance outra. */
public record AlterarNotaRequestDTO(

        @NotBlank(message = "A descrição é obrigatória (ex.: Prova 1).")
        @Size(max = 100, message = "A descrição pode ter no máximo 100 caracteres.")
        String descricao,

        @NotNull(message = "A nota é obrigatória.")
        @DecimalMin(value = "0.0", message = "A nota mínima é 0.")
        @DecimalMax(value = "10.0", message = "A nota máxima é 10.")
        @Digits(integer = 2, fraction = 2, message = "A nota pode ter no máximo 2 casas decimais.")
        BigDecimal valor

) {
}
