package com.estudante.microservico.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Disciplina (Matemática, Português...) de uma instituição. Cada nota pertence a uma disciplina. */
@Entity
public class Disciplina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    protected Disciplina() {
        // exigido pelo JPA
    }

    public Disciplina(String nome, Instituicao instituicao) {
        this.nome = nome;
        this.instituicao = instituicao;
    }

    public void renomear(String novoNome) {
        this.nome = novoNome;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Instituicao getInstituicao() {
        return instituicao;
    }

}
