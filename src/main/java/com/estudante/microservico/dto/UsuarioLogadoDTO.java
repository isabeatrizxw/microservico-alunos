package com.estudante.microservico.dto;

import com.estudante.microservico.model.Papel;
import com.estudante.microservico.security.UsuarioAutenticado;

/** Dados de quem está logado, devolvidos ao frontend. Nunca inclui a senha. */
public record UsuarioLogadoDTO(Long id, String nome, String email, Papel papel, Long instituicaoId) {

    public static UsuarioLogadoDTO de(UsuarioAutenticado usuario) {
        return new UsuarioLogadoDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPapel(), usuario.getInstituicaoId());
    }

}
