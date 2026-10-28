package com.estudante.microservico.controller;

import com.estudante.microservico.dto.AlterarNotaRequestDTO;
import com.estudante.microservico.dto.AlunoResumoDTO;
import com.estudante.microservico.dto.AtribuicaoDTO;
import com.estudante.microservico.dto.BoletimDTO;
import com.estudante.microservico.dto.MediasTurmaDTO;
import com.estudante.microservico.dto.NotaRequestDTO;
import com.estudante.microservico.dto.NotaResponseDTO;
import com.estudante.microservico.security.UsuarioAutenticado;
import com.estudante.microservico.service.NotaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Área do PROFESSOR (acesso restrito em SegurancaConfig). */
@RestController
@RequestMapping("/api/professor")
public class ProfessorController {

    private final NotaService notaService;
    private final PessoaService pessoaService;

    public ProfessorController(NotaService notaService, PessoaService pessoaService) {
        this.notaService = notaService;
        this.pessoaService = pessoaService;
    }

    /** Turmas e disciplinas atribuídas ao professor logado. */
    @GetMapping("/atribuicoes")
    public ResponseEntity<List<AtribuicaoDTO>> minhasAulas(@AuthenticationPrincipal UsuarioAutenticado professor) {
        return ResponseEntity.ok(pessoaService.atribuicoesDoProfessor(professor));
    }

    @GetMapping("/turmas/{turmaId}/alunos")
    public ResponseEntity<List<AlunoResumoDTO>> alunosDaTurma(@PathVariable Long turmaId,
                                                              @AuthenticationPrincipal UsuarioAutenticado professor) {
        return ResponseEntity.ok(notaService.alunosDaTurma(turmaId, professor));
    }

    @GetMapping("/turmas/{turmaId}/medias")
    public ResponseEntity<MediasTurmaDTO> mediasDaTurma(@PathVariable Long turmaId,
                                                        @RequestParam Long disciplinaId,
                                                        @AuthenticationPrincipal UsuarioAutenticado professor) {
        return ResponseEntity.ok(notaService.mediasDaTurma(turmaId, disciplinaId, professor));
    }

    @GetMapping("/alunos/{alunoId}/boletim")
    public ResponseEntity<BoletimDTO> boletim(@PathVariable Long alunoId,
                                              @AuthenticationPrincipal UsuarioAutenticado professor) {
        return ResponseEntity.ok(notaService.boletim(alunoId, professor));
    }

    @PostMapping("/notas")
    public ResponseEntity<NotaResponseDTO> lancar(@Valid @RequestBody NotaRequestDTO pedido,
                                                  @AuthenticationPrincipal UsuarioAutenticado professor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notaService.lancar(pedido, professor));
    }

    @PutMapping("/notas/{notaId}")
    public ResponseEntity<NotaResponseDTO> alterar(@PathVariable Long notaId,
                                                   @Valid @RequestBody AlterarNotaRequestDTO pedido,
                                                   @AuthenticationPrincipal UsuarioAutenticado professor) {
        return ResponseEntity.ok(notaService.alterar(notaId, pedido, professor));
    }

    @DeleteMapping("/notas/{notaId}")
    public ResponseEntity<Void> apagar(@PathVariable Long notaId,
                                       @AuthenticationPrincipal UsuarioAutenticado professor) {
        notaService.apagar(notaId, professor);
        return ResponseEntity.noContent().build();
    }

}
