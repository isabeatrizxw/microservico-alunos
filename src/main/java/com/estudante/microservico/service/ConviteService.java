package com.estudante.microservico.service;

import com.estudante.microservico.dto.AceitarConviteRequestDTO;
import com.estudante.microservico.dto.ConvitePublicoDTO;
import com.estudante.microservico.dto.ConviteResponseDTO;
import com.estudante.microservico.dto.NovoConviteRequestDTO;
import com.estudante.microservico.email.EnvioEmail;
import com.estudante.microservico.exception.ConflitoException;
import com.estudante.microservico.exception.NaoEncontradoException;
import com.estudante.microservico.exception.PedidoInvalidoException;
import com.estudante.microservico.model.Convite;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Turma;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.ConviteRepository;
import com.estudante.microservico.repository.InstituicaoRepository;
import com.estudante.microservico.repository.TurmaRepository;
import com.estudante.microservico.repository.UsuarioRepository;
import com.estudante.microservico.security.CodigoConvite;
import com.estudante.microservico.security.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Convites: é assim que professores e alunos entram no sistema. O ADMIN_ESCOLA define papel e turma;
 * a pessoa só escolhe nome e senha ao aceitar. A escola vem sempre do admin logado, nunca do pedido.
 */
@Service
public class ConviteService {

    static final Duration VALIDADE = Duration.ofDays(7);

    private static final String CONVITE_INVALIDO = "Convite inválido ou expirado.";

    private final ConviteRepository conviteRepository;
    private final TurmaRepository turmaRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnvioEmail envioEmail;
    private final String urlPublica;

    public ConviteService(ConviteRepository conviteRepository,
                          TurmaRepository turmaRepository,
                          InstituicaoRepository instituicaoRepository,
                          UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          EnvioEmail envioEmail,
                          @Value("${app.url-publica}") String urlPublica) {
        this.conviteRepository = conviteRepository;
        this.turmaRepository = turmaRepository;
        this.instituicaoRepository = instituicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.envioEmail = envioEmail;
        this.urlPublica = urlPublica;
    }

    /**
     * Cria o convite, envia o link por e-mail e devolve o código uma única vez (o banco guarda só o hash),
     * para quem convidou também poder copiar o link.
     */
    @Transactional
    public ConviteResponseDTO criar(NovoConviteRequestDTO pedido, UsuarioAutenticado admin) {
        if (admin.getPapel() == Papel.PROFESSOR_AUTONOMO && pedido.papel() != Papel.ALUNO) {
            throw new PedidoInvalidoException("Professor autônomo convida apenas alunos.");
        }
        Long instituicaoId = admin.getInstituicaoId();
        String email = Usuario.normalizarEmail(pedido.email());
        Turma turma = resolverTurma(pedido, instituicaoId);
        LocalDateTime agora = LocalDateTime.now();

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("Já existe uma conta com este e-mail.");
        }
        if (conviteRepository.existePendente(instituicaoId, email, agora)) {
            throw new ConflitoException("Já existe um convite pendente para este e-mail.");
        }

        String codigo = CodigoConvite.gerar();
        Convite convite = conviteRepository.save(new Convite(CodigoConvite.hash(codigo), email, pedido.papel(),
                instituicaoRepository.getReferenceById(instituicaoId), turma,
                usuarioRepository.getReferenceById(admin.getId()), agora, agora.plus(VALIDADE)));
        enviarPorEmail(convite, codigo);
        return ConviteResponseDTO.comCodigo(convite, codigo);
    }

    private void enviarPorEmail(Convite convite, String codigo) {
        String papel = convite.getPapel() == Papel.ALUNO ? "aluno" : "professor";
        String turma = convite.getTurma() == null ? "" : " da turma " + convite.getTurma().getNome();
        envioEmail.enviar(convite.getEmail(), "Convite - " + convite.getInstituicao().getNome(), """
                Olá!

                %s convidou você para entrar no Notas Escolares como %s%s.
                Para aceitar e criar sua senha, abra o link abaixo:
                %s/convite.html#%s

                O convite vale por 7 dias. Se você não esperava este convite, ignore este e-mail.
                """.formatted(convite.getInstituicao().getNome(), papel, turma, urlPublica, codigo));
    }

    @Transactional(readOnly = true)
    public List<ConviteResponseDTO> listarPendentes(UsuarioAutenticado admin) {
        return conviteRepository.findPendentes(admin.getInstituicaoId(), LocalDateTime.now()).stream()
                .map(ConviteResponseDTO::de)
                .toList();
    }

    @Transactional
    public void revogar(Long conviteId, UsuarioAutenticado admin) {
        Convite convite = conviteRepository.findByIdAndInstituicaoId(conviteId, admin.getInstituicaoId())
                .orElseThrow(() -> new NaoEncontradoException("Convite não encontrado."));
        LocalDateTime agora = LocalDateTime.now();
        if (!convite.isPendente(agora)) {
            throw new ConflitoException("Convite já usado, revogado ou expirado.");
        }
        convite.revogar(agora);
    }

    /** Público: mostra para quem recebeu o código qual escola, papel e turma o convite oferece. */
    @Transactional(readOnly = true)
    public ConvitePublicoDTO consultar(String codigo) {
        return ConvitePublicoDTO.de(buscarPendente(codigo, LocalDateTime.now()));
    }

    /** Público: cria a conta com o papel, a escola e a turma do convite, e inutiliza o código. */
    @Transactional
    public void aceitar(String codigo, AceitarConviteRequestDTO pedido) {
        LocalDateTime agora = LocalDateTime.now();
        Convite convite = buscarPendente(codigo, agora);
        if (usuarioRepository.existsByEmail(convite.getEmail())) {
            throw new ConflitoException("Já existe uma conta com este e-mail. Faça login.");
        }

        usuarioRepository.save(new Usuario(pedido.nome().trim(), convite.getEmail(),
                passwordEncoder.encode(pedido.senha()), convite.getPapel(), convite.getInstituicao(), convite.getTurma()));
        convite.marcarUsado(agora);
    }

    private Turma resolverTurma(NovoConviteRequestDTO pedido, Long instituicaoId) {
        if (pedido.papel() == Papel.PROFESSOR) {
            if (pedido.turmaId() != null) {
                throw new PedidoInvalidoException("Convite de professor não tem turma.");
            }
            return null;
        }
        if (pedido.papel() == Papel.ALUNO) {
            if (pedido.turmaId() == null) {
                throw new PedidoInvalidoException("Informe a turma do aluno.");
            }
            return turmaRepository.findByIdAndInstituicaoId(pedido.turmaId(), instituicaoId)
                    .orElseThrow(() -> new NaoEncontradoException("Turma não encontrada."));
        }
        throw new PedidoInvalidoException("Só é possível convidar PROFESSOR ou ALUNO.");
    }

    /** Código inexistente, usado, revogado ou expirado: mesma resposta, sem revelar qual é o caso. */
    private Convite buscarPendente(String codigo, LocalDateTime agora) {
        if (codigo == null || codigo.isBlank()) {
            throw new NaoEncontradoException(CONVITE_INVALIDO);
        }
        return conviteRepository.findByCodigoHash(CodigoConvite.hash(codigo))
                .filter(convite -> convite.isPendente(agora))
                .orElseThrow(() -> new NaoEncontradoException(CONVITE_INVALIDO));
    }

}
