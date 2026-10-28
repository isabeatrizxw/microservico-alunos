package com.estudante.microservico.security;

import com.estudante.microservico.exception.MuitasTentativasException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimiteDeTentativasTest {

    private static final LimiteDeTentativas.Regra TRES_POR_MINUTO = new LimiteDeTentativas.Regra(3, Duration.ofMinutes(1));

    private final RelogioAjustavel relogio = new RelogioAjustavel();
    private final LimiteDeTentativas limite = new LimiteDeTentativas(relogio);

    @Test
    void bloqueiaAoAtingirOMaximoDentroDaJanela() {
        for (int i = 0; i < 3; i++) {
            limite.consumir("chave", TRES_POR_MINUTO);
        }

        assertThrows(MuitasTentativasException.class, () -> limite.exigirDisponivel("chave", TRES_POR_MINUTO));
        assertDoesNotThrow(() -> limite.exigirDisponivel("outra-chave", TRES_POR_MINUTO));
    }

    @Test
    void liberaDepoisQueAsTentativasSaemDaJanela() {
        for (int i = 0; i < 3; i++) {
            limite.registrar("chave", TRES_POR_MINUTO);
        }

        relogio.avancar(Duration.ofSeconds(61));

        assertDoesNotThrow(() -> limite.exigirDisponivel("chave", TRES_POR_MINUTO));
    }

    @Test
    void limparZeraAContagem() {
        for (int i = 0; i < 3; i++) {
            limite.registrar("chave", TRES_POR_MINUTO);
        }

        limite.limpar("chave");

        assertDoesNotThrow(() -> limite.exigirDisponivel("chave", TRES_POR_MINUTO));
    }

    /** Relógio que o teste avança na mão. */
    private static final class RelogioAjustavel extends Clock {

        private Instant agora = Instant.parse("2026-01-01T10:00:00Z");

        void avancar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }

    }

}
