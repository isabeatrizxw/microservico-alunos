package com.estudante.microservico.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class HorarioService {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Retorna a data e hora atual do servidor no formato dd/MM/yyyy HH:mm:ss.
     *
     * @return data e hora formatada
     */
    public String horaAtual() {
        return LocalDateTime.now().format(FORMATO);
    }

}
