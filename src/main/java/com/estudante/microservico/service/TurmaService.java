package com.estudante.microservico.service;

import com.estudante.microservico.dto.TurmaRequestDTO;
import com.estudante.microservico.dto.TurmaResponseDTO;
import com.estudante.microservico.exception.ConflitoException;
import com.estudante.microservico.exception.NaoEncontradoException;
import com.estudante.microservico.repository.AtribuicaoRepository;
import com.estudante.microservico.repository.ConviteRepository;
import com.estudante.microservico.repository.UsuarioRepository;
import com.estudante.microservico.model.Instituicao;
import com.estudante.microservico.model.Turma;
import com.estudante.microservico.repository.InstituicaoRepository;
import com.estudante.microservico.repository.TurmaRepository;
import com.estudante.microservico.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Turmas da escola de quem está logado. A instituição sempre vem do usuário autenticado,
 * nunca do pedido: assim uma escola não consegue ler nem criar turmas em outra.
 */
@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtribuicaoRepository atribuicaoRepository;
    private final ConviteRepository conviteRepository;

    public TurmaService(TurmaRepository turmaRepository,
                        InstituicaoRepository instituicaoRepository,
                        UsuarioRepository usuarioRepository,
                        AtribuicaoRepository atribuicaoRepository,
                        ConviteRepository conviteRepository) {
        this.turmaRepository = turmaRepository;
        this.instituicaoRepository = instituicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.atribuicaoRepository = atribuicaoRepository;
        this.conviteRepository = conviteRepository;
    }

    @Transactional
    public TurmaResponseDTO renomear(Long turmaId, TurmaRequestDTO pedido, UsuarioAutenticado usuario) {
        Turma turma = buscar(turmaId, usuario);
        String nome = pedido.nome().trim();
        if (!turma.getNome().equalsIgnoreCase(nome)
                && turmaRepository.existsByInstituicaoIdAndNomeIgnoreCase(usuario.getInstituicaoId(), nome)) {
            throw new ConflitoException("Já existe uma turma com este nome.");
        }
        turma.renomear(nome);
        return TurmaResponseDTO.de(turma);
    }

    /** Só exclui turma vazia: com alunos, aulas atribuídas ou convites pendentes, recusa com 409. */
    @Transactional
    public void excluir(Long turmaId, UsuarioAutenticado usuario) {
        Turma turma = buscar(turmaId, usuario);
        if (usuarioRepository.existsByTurmaId(turma.getId())) {
            throw new ConflitoException("A turma tem alunos. Mude os alunos de turma antes de excluir.");
        }
        if (atribuicaoRepository.existsByTurmaId(turma.getId())) {
            throw new ConflitoException("A turma tem aulas atribuídas a professores. Remova as atribuições antes.");
        }
        if (conviteRepository.existePendenteNaTurma(turma.getId(), LocalDateTime.now())) {
            throw new ConflitoException("A turma tem convites pendentes. Revogue os convites antes de excluir.");
        }
        conviteRepository.deleteByTurmaId(turma.getId());
        turmaRepository.delete(turma);
    }

    private Turma buscar(Long turmaId, UsuarioAutenticado usuario) {
        return turmaRepository.findByIdAndInstituicaoId(turmaId, usuario.getInstituicaoId())
                .orElseThrow(() -> new NaoEncontradoException("Turma não encontrada."));
    }

    @Transactional
    public TurmaResponseDTO criar(TurmaRequestDTO pedido, UsuarioAutenticado usuario) {
        Long instituicaoId = usuario.getInstituicaoId();
        String nome = pedido.nome().trim();
        if (turmaRepository.existsByInstituicaoIdAndNomeIgnoreCase(instituicaoId, nome)) {
            throw new ConflitoException("Já existe uma turma com este nome.");
        }
        Instituicao instituicao = instituicaoRepository.getReferenceById(instituicaoId);
        return TurmaResponseDTO.de(turmaRepository.save(new Turma(nome, instituicao)));
    }

    @Transactional(readOnly = true)
    public List<TurmaResponseDTO> listar(UsuarioAutenticado usuario) {
        return turmaRepository.findByInstituicaoIdOrderByNome(usuario.getInstituicaoId()).stream()
                .map(TurmaResponseDTO::de)
                .toList();
    }

}
