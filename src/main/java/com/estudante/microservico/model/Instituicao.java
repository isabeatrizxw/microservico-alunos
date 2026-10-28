package com.estudante.microservico.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Escola cliente do sistema. Todos os dados (turmas, usuários, notas) pertencem a uma instituição,
 * e uma instituição nunca enxerga os dados de outra.
 */
@Entity
public class Instituicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoInstituicao tipo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    protected Instituicao() {
        // exigido pelo JPA
    }

    public Instituicao(String nome) {
        this(nome, TipoInstituicao.ESCOLA);
    }

    public Instituicao(String nome, TipoInstituicao tipo) {
        this.nome = nome;
        this.tipo = tipo;
        this.criadoEm = LocalDateTime.now();
    }

    public TipoInstituicao getTipo() {
        return tipo;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

}
