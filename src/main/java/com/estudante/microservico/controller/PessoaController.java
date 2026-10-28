package com.estudante.microservico.controller;

import com.estudante.microservico.dto.AtivoRequestDTO;
import com.estudante.microservico.dto.AtribuicaoDTO;
import com.estudante.microservico.dto.AtribuicaoRequestDTO;
import com.estudante.microservico.dto.MoverAlunoRequestDTO;
import com.estudante.microservico.dto.PessoasDTO;
import com.estudante.microservico.security.UsuarioAutenticado;
import com.estudante.microservico.service.PessoaService;
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

/**
 * Pessoas da escola: alunos por turma, professores e aulas atribuídas.
 * Acesso: ADMIN_ESCOLA e PROFESSOR_AUTONOMO; atribuições só ADMIN_ESCOLA (ver SegurancaConfig).
 */
@RestController
@RequestMapping("/api/escola")
public class PessoaController {

    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    @GetMapping("/pessoas")
    public ResponseEntity<PessoasDTO> listar(@AuthenticationPrincipal UsuarioAutenticado admin) {
        return ResponseEntity.ok(pessoaService.listar(admin));
    }

    @PutMapping("/alunos/{alunoId}/turma")
    public ResponseEntity<Void> moverAluno(@PathVariable Long alunoId,
                                           @Valid @RequestBody MoverAlunoRequestDTO pedido,
                                           @AuthenticationPrincipal UsuarioAutenticado admin) {
        pessoaService.moverAluno(alunoId, pedido.turmaId(), admin);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/pessoas/{usuarioId}/ativo")
    public ResponseEntity<Void> definirAtivo(@PathVariable Long usuarioId,
                                             @Valid @RequestBody AtivoRequestDTO pedido,
                                             @AuthenticationPrincipal UsuarioAutenticado admin) {
        pessoaService.definirAtivo(usuarioId, pedido.ativo(), admin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/atribuicoes")
    public ResponseEntity<AtribuicaoDTO> atribuir(@Valid @RequestBody AtribuicaoRequestDTO pedido,
                                                  @AuthenticationPrincipal UsuarioAutenticado admin) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pessoaService.atribuir(pedido, admin));
    }

    @DeleteMapping("/atribuicoes/{atribuicaoId}")
    public ResponseEntity<Void> removerAtribuicao(@PathVariable Long atribuicaoId,
                                                  @AuthenticationPrincipal UsuarioAutenticado admin) {
        pessoaService.removerAtribuicao(atribuicaoId, admin);
        return ResponseEntity.noContent().build();
    }

}
