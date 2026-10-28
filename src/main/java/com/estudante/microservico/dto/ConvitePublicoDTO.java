package com.estudante.microservico.dto;

import com.estudante.microservico.model.Convite;
import com.estudante.microservico.model.Papel;

import java.time.LocalDateTime;

/** O que a pessoa convidada vê antes de aceitar: para qual escola, papel e turma está sendo chamada. */
public record ConvitePublicoDTO(
        String email,
        Papel papel,
        String instituicaoNome,
        String turmaNome,
        LocalDateTime expiraEm
) {

    public static ConvitePublicoDTO de(Convite convite) {
        String turmaNome = convite.getTurma() == null ? null : convite.getTurma().getNome();
        return new ConvitePublicoDTO(convite.getEmail(), convite.getPapel(), convite.getInstituicao().getNome(),
                turmaNome, convite.getExpiraEm());
    }

}
