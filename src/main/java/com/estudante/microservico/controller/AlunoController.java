package com.estudante.microservico.controller;

import com.estudante.microservico.dto.BoletimDTO;
import com.estudante.microservico.security.UsuarioAutenticado;
import com.estudante.microservico.service.NotaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Área do ALUNO (acesso restrito em SegurancaConfig): só as próprias notas e médias. */
@RestController
@RequestMapping("/api/aluno")
public class AlunoController {

    private final NotaService notaService;

    public AlunoController(NotaService notaService) {
        this.notaService = notaService;
    }

    /** Notas do aluno logado por disciplina, com a média e a situação de cada uma. */
    @GetMapping("/boletim")
    public ResponseEntity<BoletimDTO> meuBoletim(@AuthenticationPrincipal UsuarioAutenticado aluno) {
        return ResponseEntity.ok(notaService.meuBoletim(aluno));
    }

}
