package com.estudante.microservico.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AlunoRequestDTO {

    @NotBlank(message = "O nome do aluno é obrigatório.")
    private String nome;

    @NotNull(message = "A nota 1 é obrigatória.")
    @DecimalMin(value = "0.0", message = "A nota mínima é 0.")
    @DecimalMax(value = "10.0", message = "A nota máxima é 10.")
    private Double nota1;

    @NotNull(message = "A nota 2 é obrigatória.")
    @DecimalMin(value = "0.0", message = "A nota mínima é 0.")
    @DecimalMax(value = "10.0", message = "A nota máxima é 10.")
    private Double nota2;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Double getNota1() {
        return nota1;
    }

    public void setNota1(Double nota1) {
        this.nota1 = nota1;
    }

    public Double getNota2() {
        return nota2;
    }

    public void setNota2(Double nota2) {
        this.nota2 = nota2;
    }

}
