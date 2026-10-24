package com.estudante.microservico.service;

import com.estudante.microservico.model.Situacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CalculoMediaServiceTest {

    private final CalculoMediaService calculo = new CalculoMediaService(new BigDecimal("5.0"));

    @Test
    void calculaMediaComDuasCasasDecimais() {
        assertEquals(new BigDecimal("8.50"), calculo.media(notas("8", "9")));
        assertEquals(new BigDecimal("7.67"), calculo.media(notas("7", "8", "8")));
        assertEquals(new BigDecimal("10.00"), calculo.media(notas("10")));
    }

    @Test
    void semNotasNaoHaMedia() {
        assertNull(calculo.media(List.of()));
        assertEquals(Situacao.SEM_NOTAS, calculo.situacao(null));
    }

    @Test
    void mediaIgualANotaDeCorteReprova() {
        assertEquals(Situacao.REPROVADO, calculo.situacao(new BigDecimal("5.00")));
        assertEquals(Situacao.REPROVADO, calculo.situacao(new BigDecimal("4.99")));
        assertEquals(Situacao.APROVADO, calculo.situacao(new BigDecimal("5.01")));
    }

    @Test
    void notaDeCorteVemDaConfiguracao() {
        CalculoMediaService corteSeis = new CalculoMediaService(new BigDecimal("6.0"));

        assertEquals(Situacao.REPROVADO, corteSeis.situacao(new BigDecimal("5.50")));
        assertEquals(Situacao.APROVADO, corteSeis.situacao(new BigDecimal("6.50")));
    }

    private static List<BigDecimal> notas(String... valores) {
        return Arrays.stream(valores).map(BigDecimal::new).toList();
    }

}
