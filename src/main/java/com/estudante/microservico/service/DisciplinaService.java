package com.estudante.microservico.service;

import com.estudante.microservico.dto.DisciplinaRequestDTO;
import com.estudante.microservico.dto.DisciplinaResponseDTO;
import com.estudante.microservico.exception.ConflitoException;
import com.estudante.microservico.exception.NaoEncontradoException;
import com.estudante.microservico.repository.AtribuicaoRepository;
import com.estudante.microservico.repository.NotaRepository;
import com.estudante.microservico.model.Disciplina;
import com.estudante.microservico.repository.DisciplinaRepository;
import com.estudante.microservico.repository.InstituicaoRepository;
import com.estudante.microservico.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Disciplinas da escola de quem está logado. A instituição vem sempre do usuário autenticado. */
@Service
public class DisciplinaService {

    private final DisciplinaRepository disciplinaRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final NotaRepository notaRepository;
    private final AtribuicaoRepository atribuicaoRepository;

    public DisciplinaService(DisciplinaRepository disciplinaRepository,
                             InstituicaoRepository instituicaoRepository,
                             NotaRepository notaRepository,
                             AtribuicaoRepository atribuicaoRepository) {
        this.disciplinaRepository = disciplinaRepository;
        this.instituicaoRepository = instituicaoRepository;
        this.notaRepository = notaRepository;
        this.atribuicaoRepository = atribuicaoRepository;
    }

    @Transactional
    public DisciplinaResponseDTO renomear(Long disciplinaId, DisciplinaRequestDTO pedido, UsuarioAutenticado usuario) {
        Disciplina disciplina = buscar(disciplinaId, usuario);
        String nome = pedido.nome().trim();
        if (!disciplina.getNome().equalsIgnoreCase(nome)
                && disciplinaRepository.existsByInstituicaoIdAndNomeIgnoreCase(usuario.getInstituicaoId(), nome)) {
            throw new ConflitoException("Já existe uma disciplina com este nome.");
        }
        disciplina.renomear(nome);
        return DisciplinaResponseDTO.de(disciplina);
    }

    /** Só exclui disciplina sem notas e sem aulas atribuídas: as notas são histórico do aluno. */
    @Transactional
    public void excluir(Long disciplinaId, UsuarioAutenticado usuario) {
        Disciplina disciplina = buscar(disciplinaId, usuario);
        if (notaRepository.existsByDisciplinaId(disciplina.getId())) {
            throw new ConflitoException("A disciplina já tem notas lançadas e não pode ser excluída.");
        }
        if (atribuicaoRepository.existsByDisciplinaId(disciplina.getId())) {
            throw new ConflitoException("A disciplina tem aulas atribuídas a professores. Remova as atribuições antes.");
        }
        disciplinaRepository.delete(disciplina);
    }

    private Disciplina buscar(Long disciplinaId, UsuarioAutenticado usuario) {
        return disciplinaRepository.findByIdAndInstituicaoId(disciplinaId, usuario.getInstituicaoId())
                .orElseThrow(() -> new NaoEncontradoException("Disciplina não encontrada."));
    }

    @Transactional
    public DisciplinaResponseDTO criar(DisciplinaRequestDTO pedido, UsuarioAutenticado usuario) {
        Long instituicaoId = usuario.getInstituicaoId();
        String nome = pedido.nome().trim();
        if (disciplinaRepository.existsByInstituicaoIdAndNomeIgnoreCase(instituicaoId, nome)) {
            throw new ConflitoException("Já existe uma disciplina com este nome.");
        }
        Disciplina disciplina = new Disciplina(nome, instituicaoRepository.getReferenceById(instituicaoId));
        return DisciplinaResponseDTO.de(disciplinaRepository.save(disciplina));
    }

    @Transactional(readOnly = true)
    public List<DisciplinaResponseDTO> listar(UsuarioAutenticado usuario) {
        return disciplinaRepository.findByInstituicaoIdOrderByNome(usuario.getInstituicaoId()).stream()
                .map(DisciplinaResponseDTO::de)
                .toList();
    }

}
