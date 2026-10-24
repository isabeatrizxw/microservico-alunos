package com.estudante.microservico.security;

import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Usuario;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Usuário logado, guardado na sessão. Leva o id da instituição para que os serviços
 * filtrem tudo pela escola de quem está logado sem consultar o banco de novo.
 */
public class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String nome;
    private final String email;
    private final Papel papel;
    private final Long instituicaoId;
    private final boolean ativo;
    private String senhaHash;

    private UsuarioAutenticado(Long id, String nome, String email, String senhaHash, Papel papel, Long instituicaoId,
                               boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.instituicaoId = instituicaoId;
        this.ativo = ativo;
    }

    public static UsuarioAutenticado de(Usuario usuario) {
        Long instituicaoId = usuario.getInstituicao() == null ? null : usuario.getInstituicao().getId();
        return new UsuarioAutenticado(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getSenhaHash(), usuario.getPapel(), instituicaoId, usuario.isAtivo());
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public Papel getPapel() {
        return papel;
    }

    /** Nulo apenas para SUPER_ADMIN. */
    public Long getInstituicaoId() {
        return instituicaoId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** Chamado pelo Spring Security após o login: o hash da senha não fica guardado na sessão. */
    @Override
    public void eraseCredentials() {
        senhaHash = null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Falso enquanto o e-mail do cadastro público não foi confirmado: o login é recusado. */
    @Override
    public boolean isEnabled() {
        return ativo;
    }

}
