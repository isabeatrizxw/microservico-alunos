package com.estudante.microservico.service;

import com.estudante.microservico.dto.AtribuicaoDTO;
import com.estudante.microservico.dto.AtribuicaoRequestDTO;
import com.estudante.microservico.dto.PessoasDTO;
import com.estudante.microservico.exception.ConflitoException;
import com.estudante.microservico.exception.NaoEncontradoException;
import com.estudante.microservico.exception.ProibidoException;
import com.estudante.microservico.model.Atribuicao;
import com.estudante.microservico.model.Disciplina;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Turma;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.AtribuicaoRepository;
import com.estudante.microservico.repository.DisciplinaRepository;
import com.estudante.microservico.repository.TurmaRepository;
import com.estudante.microservico.repository.UsuarioRepository;
import com.estudante.microservico.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestão de pessoas pela administração da escola (ou pelo professor autônomo, no espaço dele):
 * alunos por turma, professores e as aulas atribuídas a cada um. Tudo filtrado pela escola de quem está logado.
 */
@Service
public class PessoaService {

    private final UsuarioRepository usuarioRepository;
    private final TurmaRepository turmaRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final AtribuicaoRepository atribuicaoRepository;

    public PessoaService(UsuarioRepository usuarioRepository,
                         TurmaRepository turmaRepository,
                         DisciplinaRepository disciplinaRepository,
                         AtribuicaoRepository atribuicaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.turmaRepository = turmaRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.atribuicaoRepository = atribuicaoRepository;
    }

    @Transactional(readOnly = true)
    public PessoasDTO listar(UsuarioAutenticado admin) {
        Long instituicaoId = admin.getInstituicaoId();

        List<PessoasDTO.Aluno> alunos = new ArrayList<>();
        for (Usuario aluno : usuarioRepository.listarPorPapel(instituicaoId, Papel.ALUNO)) {
            Turma turma = aluno.getTurma();
            alunos.add(new PessoasDTO.Aluno(aluno.getId(), aluno.getNome(), aluno.getEmail(),
                    turma == null ? null : turma.getId(), turma == null ? null : turma.getNome(), aluno.isAtivo()));
        }

        Map<Long, List<AtribuicaoDTO>> aulasPorProfessor = new HashMap<>();
        for (Atribuicao atribuicao : atribuicaoRepository.findDaInstituicao(instituicaoId)) {
            aulasPorProfessor.computeIfAbsent(atribuicao.getProfessor().getId(), id -> new ArrayList<>())
                    .add(AtribuicaoDTO.de(atribuicao));
        }
        List<PessoasDTO.Professor> professores = new ArrayList<>();
        for (Usuario professor : usuarioRepository.listarPorPapel(instituicaoId, Papel.PROFESSOR)) {
            professores.add(new PessoasDTO.Professor(professor.getId(), professor.getNome(), professor.getEmail(),
                    professor.isAtivo(), aulasPorProfessor.getOrDefault(professor.getId(), List.of())));
        }

        return new PessoasDTO(alunos, professores);
    }

    @Transactional
    public void moverAluno(Long alunoId, Long turmaId, UsuarioAutenticado admin) {
        Usuario aluno = usuarioRepository.findByIdAndInstituicaoIdAndPapel(alunoId, admin.getInstituicaoId(), Papel.ALUNO)
                .orElseThrow(() -> new NaoEncontradoException("Aluno não encontrado."));
        aluno.mudarDeTurma(buscarTurma(turmaId, admin));
    }

    /** Desativar tira o acesso, mas mantém notas e histórico. Só vale para alunos e professores. */
    @Transactional
    public void definirAtivo(Long usuarioId, boolean ativo, UsuarioAutenticado admin) {
        Usuario pessoa = usuarioRepository.findByIdAndInstituicaoId(usuarioId, admin.getInstituicaoId())
                .orElseThrow(() -> new NaoEncontradoException("Pessoa não encontrada."));
        if (pessoa.getPapel() != Papel.ALUNO && pessoa.getPapel() != Papel.PROFESSOR) {
            throw new ProibidoException("Só é possível ativar ou desativar alunos e professores.");
        }
        if (ativo) {
            pessoa.ativar();
        } else {
            pessoa.desativar();
        }
    }

    @Transactional
    public AtribuicaoDTO atribuir(AtribuicaoRequestDTO pedido, UsuarioAutenticado admin) {
        Long instituicaoId = admin.getInstituicaoId();
        Usuario professor = usuarioRepository.findByIdAndInstituicaoIdAndPapel(pedido.professorId(), instituicaoId,
                        Papel.PROFESSOR)
                .orElseThrow(() -> new NaoEncontradoException("Professor não encontrado."));
        Turma turma = buscarTurma(pedido.turmaId(), admin);
        Disciplina disciplina = disciplinaRepository.findByIdAndInstituicaoId(pedido.disciplinaId(), instituicaoId)
                .orElseThrow(() -> new NaoEncontradoException("Disciplina não encontrada."));
        if (atribuicaoRepository.existsByProfessorIdAndTurmaIdAndDisciplinaId(professor.getId(), turma.getId(),
                disciplina.getId())) {
            throw new ConflitoException("Esse professor já dá essa disciplina nessa turma.");
        }
        return AtribuicaoDTO.de(atribuicaoRepository.save(new Atribuicao(professor, turma, disciplina)));
    }

    @Transactional
    public void removerAtribuicao(Long atribuicaoId, UsuarioAutenticado admin) {
        Atribuicao atribuicao = atribuicaoRepository.findByIdAndInstituicaoId(atribuicaoId, admin.getInstituicaoId())
                .orElseThrow(() -> new NaoEncontradoException("Atribuição não encontrada."));
        atribuicaoRepository.delete(atribuicao);
    }

    /** Aulas do professor logado (turmas e disciplinas que ele pode ver e lançar notas). */
    @Transactional(readOnly = true)
    public List<AtribuicaoDTO> atribuicoesDoProfessor(UsuarioAutenticado professor) {
        return atribuicaoRepository.findDoProfessor(professor.getId()).stream()
                .map(AtribuicaoDTO::de)
                .toList();
    }

    private Turma buscarTurma(Long turmaId, UsuarioAutenticado admin) {
        return turmaRepository.findByIdAndInstituicaoId(turmaId, admin.getInstituicaoId())
                .orElseThrow(() -> new NaoEncontradoException("Turma não encontrada."));
    }

}
