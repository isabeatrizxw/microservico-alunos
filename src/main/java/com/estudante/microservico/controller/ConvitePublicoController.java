package com.estudante.microservico.controller;

import com.estudante.microservico.dto.AceitarConviteRequestDTO;
import com.estudante.microservico.dto.ConvitePublicoDTO;
import com.estudante.microservico.service.ConviteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lado de quem recebeu o convite: não exige login (a pessoa ainda não tem conta).
 * O código de 256 bits aleatórios é a própria autorização.
 */
@RestController
@RequestMapping("/api/convites")
public class ConvitePublicoController {

    private final ConviteService conviteService;

    public ConvitePublicoController(ConviteService conviteService) {
        this.conviteService = conviteService;
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ConvitePublicoDTO> consultar(@PathVariable String codigo) {
        return ResponseEntity.ok(conviteService.consultar(codigo));
    }

    /** Cria a conta. A pessoa entra depois pelo login normal, com o e-mail do convite. */
    @PostMapping("/{codigo}/aceitar")
    public ResponseEntity<Void> aceitar(@PathVariable String codigo,
                                        @Valid @RequestBody AceitarConviteRequestDTO pedido) {
        conviteService.aceitar(codigo, pedido);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
