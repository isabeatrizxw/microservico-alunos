package com.estudante.microservico.controller;

import com.estudante.microservico.dto.InstituicaoResponseDTO;
import com.estudante.microservico.dto.NovaInstituicaoRequestDTO;
import com.estudante.microservico.service.InstituicaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Área do SUPER_ADMIN (acesso restrito em SegurancaConfig). */
@RestController
@RequestMapping("/api/admin/instituicoes")
public class InstituicaoController {

    private final InstituicaoService instituicaoService;

    public InstituicaoController(InstituicaoService instituicaoService) {
        this.instituicaoService = instituicaoService;
    }

    @PostMapping
    public ResponseEntity<InstituicaoResponseDTO> criar(@Valid @RequestBody NovaInstituicaoRequestDTO pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instituicaoService.criar(pedido));
    }

    @GetMapping
    public ResponseEntity<List<InstituicaoResponseDTO>> listar() {
        return ResponseEntity.ok(instituicaoService.listar());
    }

}
