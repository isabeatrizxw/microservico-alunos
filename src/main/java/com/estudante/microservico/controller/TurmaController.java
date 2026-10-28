package com.estudante.microservico.controller;

import com.estudante.microservico.dto.TurmaRequestDTO;
import com.estudante.microservico.dto.TurmaResponseDTO;
import com.estudante.microservico.security.UsuarioAutenticado;
import com.estudante.microservico.service.TurmaService;
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

/** Área do ADMIN_ESCOLA (acesso restrito em SegurancaConfig). */
@RestController
@RequestMapping("/api/escola/turmas")
public class TurmaController {

    private final TurmaService turmaService;

    public TurmaController(TurmaService turmaService) {
        this.turmaService = turmaService;
    }

    @PostMapping
    public ResponseEntity<TurmaResponseDTO> criar(@Valid @RequestBody TurmaRequestDTO pedido,
                                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(turmaService.criar(pedido, usuario));
    }

    @GetMapping
    public ResponseEntity<List<TurmaResponseDTO>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(turmaService.listar(usuario));
    }

    @PutMapping("/{turmaId}")
    public ResponseEntity<TurmaResponseDTO> renomear(@PathVariable Long turmaId,
                                                     @Valid @RequestBody TurmaRequestDTO pedido,
                                                     @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(turmaService.renomear(turmaId, pedido, usuario));
    }

    @DeleteMapping("/{turmaId}")
    public ResponseEntity<Void> excluir(@PathVariable Long turmaId,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        turmaService.excluir(turmaId, usuario);
        return ResponseEntity.noContent().build();
    }

}
