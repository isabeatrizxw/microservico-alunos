package com.estudante.microservico.dto;

import com.estudante.microservico.model.Instituicao;
import com.estudante.microservico.model.TipoInstituicao;

import java.time.LocalDateTime;

public record InstituicaoResponseDTO(Long id, String nome, TipoInstituicao tipo, LocalDateTime criadoEm) {

    public static InstituicaoResponseDTO de(Instituicao instituicao) {
        return new InstituicaoResponseDTO(instituicao.getId(), instituicao.getNome(), instituicao.getTipo(),
                instituicao.getCriadoEm());
    }

}
