package com.estudante.microservico.dto;

import com.estudante.microservico.model.Turma;

public record TurmaResponseDTO(Long id, String nome) {

    public static TurmaResponseDTO de(Turma turma) {
        return new TurmaResponseDTO(turma.getId(), turma.getNome());
    }

}
