package com.estudante.microservico.service;

import com.estudante.microservico.dto.AlterarNotaRequestDTO;
import com.estudante.microservico.dto.AlunoResumoDTO;
import com.estudante.microservico.dto.BoletimDTO;
import com.estudante.microservico.dto.MediasTurmaDTO;
import com.estudante.microservico.dto.NotaRequestDTO;
import com.estudante.microservico.dto.NotaResponseDTO;
import com.estudante.microservico.exception.NaoEncontradoException;
import com.estudante.microservico.exception.PedidoInvalidoException;
import com.estudante.microservico.repository.AtribuicaoRepository;
import com.estudante.microservico.exception.ProibidoException;
import com.estudante.microservico.model.Disciplina;
import com.estudante.microservico.model.Nota;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Situacao;
import com.estudante.microservico.model.Turma;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.DisciplinaRepository;
import com.estudante.microservico.repository.NotaRepository;
import com.estudante.microservico.repository.TurmaRepository;
import com.estudante.microservico.repository.UsuarioRepository;
import com.estudante.microservico.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notas, boletim e média da turma. Tudo é filtrado pela escola de quem está logado:
 * aluno, turma, disciplina ou nota de outra escola responde 404, como se não existisse.
 */
@Service
public class NotaService {

    private final NotaRepository notaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TurmaRepository turmaRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final CalculoMediaService calculoMedia;
    private final AtribuicaoRepository atribuicaoRepository;

    public NotaService(NotaRepository notaRepository,
                       UsuarioRepository usuarioRepository,
                       TurmaRepository turmaRepository,
                       DisciplinaRepository disciplinaRepository,
                       CalculoMediaService calculoMedia,
                       AtribuicaoRepository atribuicaoRepository) {
        this.notaRepository = notaRepository;
        this.usuarioRepository = usuarioRepository;
        this.turmaRepository = turmaRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.calculoMedia = calculoMedia;
        this.atribuicaoRepository = atribuicaoRepository;
    }

    @Transactional(readOnly = true)
    public List<AlunoResumoDTO> alunosDaTurma(Long turmaId, UsuarioAutenticado professor) {
        Turma turma = buscarTurma(turmaId, professor);
        exigirTurma(professor, turma.getId());
        return usuarioRepository.findByTurmaIdAndAtivoTrueOrderByNome(turma.getId()).stream()
                .map(AlunoResumoDTO::de)
                .toList();
    }

    @Transactional
    public NotaResponseDTO lancar(NotaRequestDTO pedido, UsuarioAutenticado professor) {
        Usuario aluno = buscarAluno(pedido.alunoId(), professor);
        Disciplina disciplina = buscarDisciplina(pedido.disciplinaId(), professor);
        exigirAula(professor, aluno.getTurma() == null ? null : aluno.getTurma().getId(), disciplina.getId());
        if (!aluno.isAtivo()) {
            throw new PedidoInvalidoException("Aluno desativado: não recebe notas novas.");
        }
        Usuario autor = usuarioRepository.getReferenceById(professor.getId());

        Nota nota = new Nota(aluno, disciplina, pedido.descricao().trim(), pedido.valor(), autor, LocalDateTime.now());
        return NotaResponseDTO.de(notaRepository.save(nota));
    }

    @Transactional
    public NotaResponseDTO alterar(Long notaId, AlterarNotaRequestDTO pedido, UsuarioAutenticado professor) {
        Nota nota = buscarNotaDoAutor(notaId, professor);
        nota.alterar(pedido.descricao().trim(), pedido.valor(), LocalDateTime.now());
        return NotaResponseDTO.de(nota);
    }

    @Transactional
    public void apagar(Long notaId, UsuarioAutenticado professor) {
        notaRepository.delete(buscarNotaDoAutor(notaId, professor));
    }

    /** Boletim de um aluno da escola (visão do professor). */
    @Transactional(readOnly = true)
    public BoletimDTO boletim(Long alunoId, UsuarioAutenticado professor) {
        Usuario aluno = buscarAluno(alunoId, professor);
        exigirTurma(professor, aluno.getTurma() == null ? null : aluno.getTurma().getId());
        return montarBoletim(aluno);
    }

    /** Boletim de quem está logado (visão do aluno): só as próprias notas. */
    @Transactional(readOnly = true)
    public BoletimDTO meuBoletim(UsuarioAutenticado aluno) {
        return montarBoletim(buscarAluno(aluno.getId(), aluno));
    }

    /** "Média da sala": média de cada aluno da turma na disciplina e a média da turma, calculadas com for. */
    @Transactional(readOnly = true)
    public MediasTurmaDTO mediasDaTurma(Long turmaId, Long disciplinaId, UsuarioAutenticado professor) {
        Turma turma = buscarTurma(turmaId, professor);
        Disciplina disciplina = buscarDisciplina(disciplinaId, professor);
        exigirAula(professor, turma.getId(), disciplina.getId());
        List<Usuario> alunos = usuarioRepository.findByTurmaIdAndAtivoTrueOrderByNome(turma.getId());

        Map<Long, List<BigDecimal>> notasPorAluno = new HashMap<>();
        for (Nota nota : notaRepository.findDaTurmaNaDisciplina(turma.getId(), disciplina.getId())) {
            notasPorAluno.computeIfAbsent(nota.getAluno().getId(), id -> new ArrayList<>()).add(nota.getValor());
        }

        List<MediasTurmaDTO.MediaAluno> medias = new ArrayList<>();
        List<BigDecimal> mediasComNotas = new ArrayList<>();
        int aprovados = 0;
        int reprovados = 0;
        int semNotas = 0;
        for (int i = 0; i < alunos.size(); i++) {
            Usuario aluno = alunos.get(i);
            List<BigDecimal> notas = notasPorAluno.getOrDefault(aluno.getId(), List.of());
            BigDecimal media = calculoMedia.media(notas);
            Situacao situacao = calculoMedia.situacao(media);

            medias.add(new MediasTurmaDTO.MediaAluno(aluno.getId(), aluno.getNome(), notas.size(), media, situacao));
            if (situacao == Situacao.APROVADO) {
                aprovados++;
            } else if (situacao == Situacao.REPROVADO) {
                reprovados++;
            } else {
                semNotas++;
            }
            if (media != null) {
                mediasComNotas.add(media);
            }
        }

        return new MediasTurmaDTO(turma.getId(), turma.getNome(), disciplina.getId(), disciplina.getNome(),
                medias, calculoMedia.media(mediasComNotas), aprovados, reprovados, semNotas, LocalDateTime.now());
    }

    private BoletimDTO montarBoletim(Usuario aluno) {
        Map<Long, List<Nota>> notasPorDisciplina = new HashMap<>();
        for (Nota nota : notaRepository.findBoletim(aluno.getId())) {
            notasPorDisciplina.computeIfAbsent(nota.getDisciplina().getId(), id -> new ArrayList<>()).add(nota);
        }

        List<BoletimDTO.PorDisciplina> disciplinas = new ArrayList<>();
        for (Disciplina disciplina : disciplinaRepository.findByInstituicaoIdOrderByNome(aluno.getInstituicao().getId())) {
            List<Nota> notas = notasPorDisciplina.getOrDefault(disciplina.getId(), List.of());
            List<BigDecimal> valores = new ArrayList<>();
            List<NotaResponseDTO> notasDto = new ArrayList<>();
            for (Nota nota : notas) {
                valores.add(nota.getValor());
                notasDto.add(NotaResponseDTO.de(nota));
            }
            BigDecimal media = calculoMedia.media(valores);
            disciplinas.add(new BoletimDTO.PorDisciplina(disciplina.getId(), disciplina.getNome(), notasDto,
                    media, calculoMedia.situacao(media)));
        }

        String turmaNome = aluno.getTurma() == null ? null : aluno.getTurma().getNome();
        return new BoletimDTO(aluno.getId(), aluno.getNome(), turmaNome, disciplinas, LocalDateTime.now());
    }

    private Usuario buscarAluno(Long alunoId, UsuarioAutenticado usuario) {
        return usuarioRepository.findByIdAndInstituicaoIdAndPapel(alunoId, usuario.getInstituicaoId(), Papel.ALUNO)
                .orElseThrow(() -> new NaoEncontradoException("Aluno não encontrado."));
    }

    private Turma buscarTurma(Long turmaId, UsuarioAutenticado usuario) {
        return turmaRepository.findByIdAndInstituicaoId(turmaId, usuario.getInstituicaoId())
                .orElseThrow(() -> new NaoEncontradoException("Turma não encontrada."));
    }

    private Disciplina buscarDisciplina(Long disciplinaId, UsuarioAutenticado usuario) {
        return disciplinaRepository.findByIdAndInstituicaoId(disciplinaId, usuario.getInstituicaoId())
                .orElseThrow(() -> new NaoEncontradoException("Disciplina não encontrada."));
    }

    /**
     * Professor de escola só atua nas turmas em que tem aula atribuída. O professor autônomo
     * é dono do próprio espaço e não passa por essa checagem.
     */
    private void exigirTurma(UsuarioAutenticado professor, Long turmaId) {
        if (professor.getPapel() == Papel.PROFESSOR_AUTONOMO) {
            return;
        }
        if (turmaId == null || !atribuicaoRepository.existsByProfessorIdAndTurmaId(professor.getId(), turmaId)) {
            throw new ProibidoException("Você não dá aula nesta turma.");
        }
    }

    /** Lançar nota e ver a média da turma exigem a disciplina atribuída naquela turma. */
    private void exigirAula(UsuarioAutenticado professor, Long turmaId, Long disciplinaId) {
        if (professor.getPapel() == Papel.PROFESSOR_AUTONOMO) {
            return;
        }
        if (turmaId == null
                || !atribuicaoRepository.existsByProfessorIdAndTurmaIdAndDisciplinaId(professor.getId(), turmaId, disciplinaId)) {
            throw new ProibidoException("Você não dá esta disciplina nesta turma.");
        }
    }

    private Nota buscarNotaDoAutor(Long notaId, UsuarioAutenticado professor) {
        Nota nota = notaRepository.findByIdAndInstituicaoId(notaId, professor.getInstituicaoId())
                .orElseThrow(() -> new NaoEncontradoException("Nota não encontrada."));
        if (!nota.foiLancadaPor(professor.getId())) {
            throw new ProibidoException("Apenas o professor que lançou a nota pode alterá-la ou apagá-la.");
        }
        return nota;
    }

}
