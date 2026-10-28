package com.estudante.microservico.service;

import com.estudante.microservico.model.Situacao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Cálculo de médias e da situação (aprovado/reprovado) em relação à nota de corte.
 * A nota de corte vem de {@code aluno.nota-de-corte} e pode ser alterada sem recompilar.
 */
@Service
public class CalculoMediaService {

    private static final int CASAS_DECIMAIS = 2;

    private final BigDecimal notaDeCorte;

    public CalculoMediaService(@Value("${aluno.nota-de-corte:5.0}") BigDecimal notaDeCorte) {
        this.notaDeCorte = notaDeCorte;
    }

    /**
     * Média aritmética, somando os valores com um laço for. Arredonda para 2 casas.
     *
     * @return a média, ou {@code null} se a lista estiver vazia (não há média sem notas)
     */
    public BigDecimal media(List<BigDecimal> valores) {
        if (valores.isEmpty()) {
            return null;
        }
        BigDecimal soma = BigDecimal.ZERO;
        for (int i = 0; i < valores.size(); i++) {
            soma = soma.add(valores.get(i));
        }
        return soma.divide(BigDecimal.valueOf(valores.size()), CASAS_DECIMAIS, RoundingMode.HALF_UP);
    }

    /** Acima da nota de corte aprova; igual ou abaixo reprova; sem média, SEM_NOTAS. */
    public Situacao situacao(BigDecimal media) {
        if (media == null) {
            return Situacao.SEM_NOTAS;
        }
        return media.compareTo(notaDeCorte) > 0 ? Situacao.APROVADO : Situacao.REPROVADO;
    }

}
