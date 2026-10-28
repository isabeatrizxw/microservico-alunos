package com.estudante.microservico.controller;

import com.estudante.microservico.dto.ConviteResponseDTO;
import com.estudante.microservico.dto.NovoConviteRequestDTO;
import com.estudante.microservico.security.UsuarioAutenticado;
import com.estudante.microservico.service.ConviteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Convites gerenciados pelo ADMIN_ESCOLA (acesso restrito em SegurancaConfig). */
@RestController
@RequestMapping("/api/escola/convites")
public class ConviteEscolaController {

    private final ConviteService conviteService;

    public ConviteEscolaController(ConviteService conviteService) {
        this.conviteService = conviteService;
    }

    @PostMapping
    public ResponseEntity<ConviteResponseDTO> criar(@Valid @RequestBody NovoConviteRequestDTO pedido,
                                                    @AuthenticationPrincipal UsuarioAutenticado admin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conviteService.criar(pedido, admin));
    }

    @GetMapping
    public ResponseEntity<List<ConviteResponseDTO>> listarPendentes(@AuthenticationPrincipal UsuarioAutenticado admin) {
        return ResponseEntity.ok(conviteService.listarPendentes(admin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revogar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado admin) {
        conviteService.revogar(id, admin);
        return ResponseEntity.noContent().build();
    }

}
