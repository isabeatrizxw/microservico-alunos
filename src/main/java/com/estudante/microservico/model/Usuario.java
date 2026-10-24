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
import java.util.Locale;
import java.util.Objects;

/**
 * Pessoa que acessa o sistema. As regras de vínculo são verificadas aqui e repetidas
 * como CHECK no banco (migração V1):
 * <ul>
 *   <li>SUPER_ADMIN não pertence a instituição; os demais papéis pertencem a uma;</li>
 *   <li>só ALUNO tem turma, e a turma precisa ser da mesma instituição do aluno.</li>
 * </ul>
 */
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turma_id")
    private Turma turma;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    /** Falso enquanto o e-mail não é confirmado (cadastro público). Conta inativa não consegue entrar. */
    @Column(nullable = false)
    private boolean ativo;

    protected Usuario() {
        // exigido pelo JPA
    }

    /**
     * @param senhaHash senha já criptografada (BCrypt) — nunca a senha em texto puro
     * @param turma     usada apenas para ALUNO (pode ser nula se o aluno ainda não foi enturmado)
     */
    public Usuario(String nome, String email, String senhaHash, Papel papel, Instituicao instituicao, Turma turma) {
        validarVinculos(papel, instituicao, turma);
        this.nome = nome;
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.instituicao = instituicao;
        this.turma = turma;
        this.criadoEm = LocalDateTime.now();
        this.ativo = true;
    }

    /** Conta criada pelo cadastro público: só entra depois de confirmar o e-mail. */
    public void aguardarConfirmacaoDeEmail() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    /** Tira o acesso sem apagar nada: notas lançadas ou recebidas continuam no histórico. */
    public void desativar() {
        this.ativo = false;
    }

    /** Muda o aluno de turma (ex.: entrou na turma errada ou passou de ano). */
    public void mudarDeTurma(Turma novaTurma) {
        if (papel != Papel.ALUNO) {
            throw new IllegalArgumentException("Apenas alunos são vinculados a uma turma.");
        }
        if (!mesmaInstituicao(novaTurma.getInstituicao(), instituicao)) {
            throw new IllegalArgumentException("A turma precisa ser da mesma instituição do aluno.");
        }
        this.turma = novaTurma;
    }

    /**
     * Novo cadastro público com o e-mail de uma conta ainda não confirmada: vale o último pedido.
     * Assim, se alguém cadastrou o e-mail de outra pessoa, o dono real do e-mail ainda consegue
     * se cadastrar com a própria senha (e quem tentou antes não tem acesso ao link enviado).
     */
    public void atualizarCadastroPendente(String novoNome, String novaSenhaHash) {
        if (ativo) {
            throw new IllegalStateException("Conta já confirmada não pode ser recadastrada.");
        }
        this.nome = novoNome;
        this.senhaHash = novaSenhaHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    /** E-mail é guardado sempre em minúsculas e sem espaços, para o login não depender de maiúsculas. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validarVinculos(Papel papel, Instituicao instituicao, Turma turma) {
        Objects.requireNonNull(papel, "O papel é obrigatório.");
        if (papel == Papel.SUPER_ADMIN && instituicao != null) {
            throw new IllegalArgumentException("SUPER_ADMIN não pertence a uma instituição.");
        }
        if (papel != Papel.SUPER_ADMIN && instituicao == null) {
            throw new IllegalArgumentException("Usuário " + papel + " precisa de uma instituição.");
        }
        if (turma == null) {
            return;
        }
        if (papel != Papel.ALUNO) {
            throw new IllegalArgumentException("Apenas alunos são vinculados a uma turma.");
        }
        if (!mesmaInstituicao(turma.getInstituicao(), instituicao)) {
            throw new IllegalArgumentException("A turma precisa ser da mesma instituição do aluno.");
        }
    }

    private static boolean mesmaInstituicao(Instituicao a, Instituicao b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
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

    public String getSenhaHash() {
        return senhaHash;
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

}
