package com.estudante.microservico.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Aula atribuída pela administração da escola: o professor dá esta disciplina nesta turma.
 * O professor de escola só vê e lança notas nas turmas e disciplinas atribuídas a ele.
 */
@Entity
public class Atribuicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Usuario professor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turma_id", nullable = false)
    private Turma turma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disciplina_id", nullable = false)
    private Disciplina disciplina;

    protected Atribuicao() {
        // exigido pelo JPA
    }

    public Atribuicao(Usuario professor, Turma turma, Disciplina disciplina) {
        if (professor.getPapel() != Papel.PROFESSOR) {
            throw new IllegalArgumentException("Aulas só podem ser atribuídas a professores da escola.");
        }
        Long instituicaoId = professor.getInstituicao().getId();
        if (!instituicaoId.equals(turma.getInstituicao().getId())
                || !instituicaoId.equals(disciplina.getInstituicao().getId())) {
            throw new IllegalArgumentException("Professor, turma e disciplina precisam ser da mesma instituição.");
        }
        this.instituicao = professor.getInstituicao();
        this.professor = professor;
        this.turma = turma;
        this.disciplina = disciplina;
    }

    public Long getId() {
        return id;
    }

    public Usuario getProfessor() {
        return professor;
    }

    public Turma getTurma() {
        return turma;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }

}
