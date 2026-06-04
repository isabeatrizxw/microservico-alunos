package com.estudante.microservico.controller;

import com.estudante.microservico.service.HorarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @GetMapping("/datetime")
    public ResponseEntity<Map<String, String>> dataHora() {
        return ResponseEntity.ok(Map.of("dataHora", horarioService.horaAtual()));
    }

}
