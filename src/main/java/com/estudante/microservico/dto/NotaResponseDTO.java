package com.estudante.microservico.dto;

import com.estudante.microservico.model.Nota;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NotaResponseDTO(
        Long id,
        Long alunoId,
        String alunoNome,
        Long disciplinaId,
        String disciplinaNome,
        String descricao,
        BigDecimal valor,
        Long lancadaPorId,
        String lancadaPorNome,
        LocalDateTime lancadaEm,
        LocalDateTime atualizadaEm
) {

    public static NotaResponseDTO de(Nota nota) {
        return new NotaResponseDTO(nota.getId(),
                nota.getAluno().getId(), nota.getAluno().getNome(),
                nota.getDisciplina().getId(), nota.getDisciplina().getNome(),
                nota.getDescricao(), nota.getValor(),
                nota.getLancadaPor().getId(), nota.getLancadaPor().getNome(),
                nota.getLancadaEm(), nota.getAtualizadaEm());
    }

}
