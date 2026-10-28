package com.estudante.microservico.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Convite para um professor ou aluno entrar numa escola. O papel, a escola e a turma são definidos
 * pelo ADMIN_ESCOLA aqui; quem aceita só escolhe o próprio nome e senha.
 * Guarda apenas o hash do código (ver CodigoConvite), nunca o código em si.
 */
@Entity
public class Convite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_hash", nullable = false, unique = true, length = 64)
    private String codigoHash;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turma_id")
    private Turma turma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private Usuario criadoPor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    @Column(name = "revogado_em")
    private LocalDateTime revogadoEm;

    protected Convite() {
        // exigido pelo JPA
    }

    public Convite(String codigoHash, String email, Papel papel, Instituicao instituicao, Turma turma,
                   Usuario criadoPor, LocalDateTime criadoEm, LocalDateTime expiraEm) {
        Objects.requireNonNull(papel, "O papel é obrigatório.");
        if (papel != Papel.PROFESSOR && papel != Papel.ALUNO) {
            throw new IllegalArgumentException("Convites são apenas para professores e alunos.");
        }
        if (papel == Papel.ALUNO && turma == null) {
            throw new IllegalArgumentException("Convite de aluno precisa de uma turma.");
        }
        if (papel != Papel.ALUNO && turma != null) {
            throw new IllegalArgumentException("Apenas convites de aluno têm turma.");
        }
        this.codigoHash = codigoHash;
        this.email = Usuario.normalizarEmail(email);
        this.papel = papel;
        this.instituicao = instituicao;
        this.turma = turma;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    /** Pendente = ainda não usado, não revogado e dentro da validade. */
    public boolean isPendente(LocalDateTime agora) {
        return usadoEm == null && revogadoEm == null && agora.isBefore(expiraEm);
    }

    public void marcarUsado(LocalDateTime agora) {
        exigirPendente(agora);
        this.usadoEm = agora;
    }

    public void revogar(LocalDateTime agora) {
        exigirPendente(agora);
        this.revogadoEm = agora;
    }

    private void exigirPendente(LocalDateTime agora) {
        if (!isPendente(agora)) {
            throw new IllegalStateException("Convite já usado, revogado ou expirado.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Papel getPapel() {
        return papel;
    }

    public Instituicao getInstituicao() {
        return instituicao;
    }

    public Turma getTurma() {
        return turma;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

}
