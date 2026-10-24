package com.estudante.microservico.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Nota de um aluno numa disciplina, lançada por um professor da mesma escola.
 * Só o professor que lançou pode alterar ou apagar (regra aplicada em NotaService).
 */
@Entity
public class Nota {

    public static final BigDecimal VALOR_MINIMO = BigDecimal.ZERO;
    public static final BigDecimal VALOR_MAXIMO = BigDecimal.TEN;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disciplina_id", nullable = false)
    private Disciplina disciplina;

    @Column(nullable = false, length = 100)
    private String descricao;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lancada_por_id", nullable = false)
    private Usuario lancadaPor;

    @Column(name = "lancada_em", nullable = false)
    private LocalDateTime lancadaEm;

    @Column(name = "atualizada_em")
    private LocalDateTime atualizadaEm;

    protected Nota() {
        // exigido pelo JPA
    }

    public Nota(Usuario aluno, Disciplina disciplina, String descricao, BigDecimal valor,
                Usuario lancadaPor, LocalDateTime lancadaEm) {
        if (aluno.getPapel() != Papel.ALUNO) {
            throw new IllegalArgumentException("Notas só podem ser lançadas para alunos.");
        }
        if (lancadaPor.getPapel() != Papel.PROFESSOR && lancadaPor.getPapel() != Papel.PROFESSOR_AUTONOMO) {
            throw new IllegalArgumentException("Apenas professores lançam notas.");
        }
        Long instituicaoId = aluno.getInstituicao().getId();
        if (!instituicaoId.equals(disciplina.getInstituicao().getId())
                || !instituicaoId.equals(lancadaPor.getInstituicao().getId())) {
            throw new IllegalArgumentException("Aluno, disciplina e professor precisam ser da mesma instituição.");
        }
        validarValor(valor);
        this.instituicao = aluno.getInstituicao();
        this.aluno = aluno;
        this.disciplina = disciplina;
        this.descricao = descricao;
        this.valor = valor;
        this.lancadaPor = lancadaPor;
        this.lancadaEm = lancadaEm;
    }

    public void alterar(String novaDescricao, BigDecimal novoValor, LocalDateTime agora) {
        validarValor(novoValor);
        this.descricao = novaDescricao;
        this.valor = novoValor;
        this.atualizadaEm = agora;
    }

    public boolean foiLancadaPor(Long usuarioId) {
        return lancadaPor.getId().equals(usuarioId);
    }

    private static void validarValor(BigDecimal valor) {
        Objects.requireNonNull(valor, "O valor da nota é obrigatório.");
        if (valor.compareTo(VALOR_MINIMO) < 0 || valor.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("A nota precisa estar entre 0 e 10.");
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getAluno() {
        return aluno;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Usuario getLancadaPor() {
        return lancadaPor;
    }

    public LocalDateTime getLancadaEm() {
        return lancadaEm;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }

}
