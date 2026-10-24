package com.estudante.microservico.security;

import com.estudante.microservico.exception.MuitasTentativasException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas por chave (ex.: "login-email:ana@x.com", "cadastro-ip:1.2.3.4") numa janela de tempo,
 * contra robôs testando senhas ou criando contas em massa.
 *
 * <p>Fica na memória deste servidor: se o sistema rodar em várias instâncias, cada uma conta separado,
 * e reiniciar zera a contagem. Para várias instâncias, trocar por um contador compartilhado (ex.: Redis).
 */
@Component
public class LimiteDeTentativas {

    /** Quantas tentativas cabem em cada janela de tempo. */
    public record Regra(int maximo, Duration janela) {
    }

    private static final int CHAVES_ANTES_DE_LIMPAR = 10_000;
    private static final Duration JANELA_MAIS_LONGA = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> tentativas = new ConcurrentHashMap<>();
    private final Clock relogio;

    @Autowired
    public LimiteDeTentativas() {
        this(Clock.systemUTC());
    }

    LimiteDeTentativas(Clock relogio) {
        this.relogio = relogio;
    }

    /** Lança MuitasTentativasException se a chave já atingiu o máximo dentro da janela. */
    public void exigirDisponivel(String chave, Regra regra) {
        Deque<Instant> fila = tentativas.get(chave);
        if (fila == null) {
            return;
        }
        synchronized (fila) {
            descartarAntigas(fila, regra.janela());
            if (fila.size() >= regra.maximo()) {
                throw new MuitasTentativasException();
            }
        }
    }

    /** Conta uma tentativa para a chave. */
    public void registrar(String chave, Regra regra) {
        Deque<Instant> fila = tentativas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            descartarAntigas(fila, regra.janela());
            fila.addLast(relogio.instant());
        }
        if (tentativas.size() > CHAVES_ANTES_DE_LIMPAR) {
            limparChavesAntigas();
        }
    }

    /** Verifica e já conta a tentativa (para ações limitadas mesmo quando dão certo, como cadastro). */
    public void consumir(String chave, Regra regra) {
        exigirDisponivel(chave, regra);
        registrar(chave, regra);
    }

    /** Esquece a chave (ex.: login certo zera as falhas daquele e-mail). */
    public void limpar(String chave) {
        tentativas.remove(chave);
    }

    private void descartarAntigas(Deque<Instant> fila, Duration janela) {
        Instant limite = relogio.instant().minus(janela);
        while (!fila.isEmpty() && fila.peekFirst().isBefore(limite)) {
            fila.pollFirst();
        }
    }

    private void limparChavesAntigas() {
        Instant limite = relogio.instant().minus(JANELA_MAIS_LONGA);
        tentativas.entrySet().removeIf(entrada -> {
            synchronized (entrada.getValue()) {
                Instant ultima = entrada.getValue().peekLast();
                return ultima == null || ultima.isBefore(limite);
            }
        });
    }

}
