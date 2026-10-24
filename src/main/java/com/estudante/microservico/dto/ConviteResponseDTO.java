package com.estudante.microservico.dto;

import com.estudante.microservico.model.Convite;
import com.estudante.microservico.model.Papel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Convite visto pelo ADMIN_ESCOLA. O {@code codigo} só é preenchido na resposta da criação:
 * depois disso o servidor não tem mais como mostrá-lo (guarda apenas o hash).
 */
public record ConviteResponseDTO(
        Long id,
        String email,
        Papel papel,
        Long turmaId,
        String turmaNome,
        LocalDateTime criadoEm,
        LocalDateTime expiraEm,
        @JsonInclude(JsonInclude.Include.NON_NULL) String codigo
) {

    public static ConviteResponseDTO de(Convite convite) {
        return comCodigo(convite, null);
    }

    public static ConviteResponseDTO comCodigo(Convite convite, String codigo) {
        Long turmaId = convite.getTurma() == null ? null : convite.getTurma().getId();
        String turmaNome = convite.getTurma() == null ? null : convite.getTurma().getNome();
        return new ConviteResponseDTO(convite.getId(), convite.getEmail(), convite.getPapel(), turmaId, turmaNome,
                convite.getCriadoEm(), convite.getExpiraEm(), codigo);
    }

}
