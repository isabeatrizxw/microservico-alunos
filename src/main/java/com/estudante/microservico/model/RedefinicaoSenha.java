package com.estudante.microservico.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

/**
 * Link de "esqueci minha senha". Como nos convites, guarda só o hash do código (ver CodigoConvite):
 * quem ler o banco não consegue trocar a senha de ninguém.
 */
@Entity
public class RedefinicaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "codigo_hash", nullable = false, unique = true, length = 64)
    private String codigoHash;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    protected RedefinicaoSenha() {
        // exigido pelo JPA
    }

    public RedefinicaoSenha(Usuario usuario, String codigoHash, LocalDateTime criadoEm, LocalDateTime expiraEm) {
        this.usuario = usuario;
        this.codigoHash = codigoHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean isPendente(LocalDateTime agora) {
        return usadoEm == null && agora.isBefore(expiraEm);
    }

    /** Usa o link e troca a senha. */
    public void usar(LocalDateTime agora, String novaSenhaHash) {
        if (!isPendente(agora)) {
            throw new IllegalStateException("Link de redefinição já usado ou expirado.");
        }
        this.usadoEm = agora;
        usuario.trocarSenha(novaSenhaHash);
    }

    public Usuario getUsuario() {
        return usuario;
    }

}
