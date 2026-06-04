package com.estudante.microservico.controller;

import com.estudante.microservico.dto.AlunoRequestDTO;
import com.estudante.microservico.dto.AlunoResponseDTO;
import com.estudante.microservico.service.AlunoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @PostMapping("/media")
    public ResponseEntity<AlunoResponseDTO> calcularMedia(@Valid @RequestBody AlunoRequestDTO request) {
        return ResponseEntity.ok(alunoService.calcularMedia(request));
    }

}
