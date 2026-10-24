package com.estudante.microservico.dto;

import com.estudante.microservico.model.Disciplina;

public record DisciplinaResponseDTO(Long id, String nome) {

    public static DisciplinaResponseDTO de(Disciplina disciplina) {
        return new DisciplinaResponseDTO(disciplina.getId(), disciplina.getNome());
    }

}
