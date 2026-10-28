package com.estudante.microservico.controller;

import com.estudante.microservico.dto.DisciplinaRequestDTO;
import com.estudante.microservico.dto.DisciplinaResponseDTO;
import com.estudante.microservico.security.UsuarioAutenticado;
import com.estudante.microservico.service.DisciplinaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** ADMIN_ESCOLA cria disciplinas; ADMIN_ESCOLA e PROFESSOR listam (regras em SegurancaConfig). */
@RestController
@RequestMapping("/api/escola/disciplinas")
public class DisciplinaController {

    private final DisciplinaService disciplinaService;

    public DisciplinaController(DisciplinaService disciplinaService) {
        this.disciplinaService = disciplinaService;
    }

    @PostMapping
    public ResponseEntity<DisciplinaResponseDTO> criar(@Valid @RequestBody DisciplinaRequestDTO pedido,
                                                       @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disciplinaService.criar(pedido, usuario));
    }

    @GetMapping
    public ResponseEntity<List<DisciplinaResponseDTO>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(disciplinaService.listar(usuario));
    }

    @PutMapping("/{disciplinaId}")
    public ResponseEntity<DisciplinaResponseDTO> renomear(@PathVariable Long disciplinaId,
                                                          @Valid @RequestBody DisciplinaRequestDTO pedido,
                                                          @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(disciplinaService.renomear(disciplinaId, pedido, usuario));
    }

    @DeleteMapping("/{disciplinaId}")
    public ResponseEntity<Void> excluir(@PathVariable Long disciplinaId,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        disciplinaService.excluir(disciplinaId, usuario);
        return ResponseEntity.noContent().build();
    }

}
