package com.estudante.microservico.service;

import com.estudante.microservico.dto.CadastroAutonomoRequestDTO;
import com.estudante.microservico.email.EnvioEmail;
import com.estudante.microservico.exception.NaoEncontradoException;
import com.estudante.microservico.model.ConfirmacaoEmail;
import com.estudante.microservico.model.Instituicao;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.TipoInstituicao;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.ConfirmacaoEmailRepository;
import com.estudante.microservico.repository.InstituicaoRepository;
import com.estudante.microservico.repository.UsuarioRepository;
import com.estudante.microservico.security.CodigoConvite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cadastro público de professor autônomo: cria uma "escola de uma pessoa só" e a conta do professor,
 * que só consegue entrar depois de confirmar o e-mail.
 *
 * <p>Para não revelar quem já tem conta, a resposta é a mesma em todos os casos; a diferença
 * vai por e-mail, que só o dono do endereço lê.
 */
@Service
public class CadastroService {

    static final Duration VALIDADE_CONFIRMACAO = Duration.ofHours(24);

    private final UsuarioRepository usuarioRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final ConfirmacaoEmailRepository confirmacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnvioEmail envioEmail;
    private final String urlPublica;

    public CadastroService(UsuarioRepository usuarioRepository,
                           InstituicaoRepository instituicaoRepository,
                           ConfirmacaoEmailRepository confirmacaoRepository,
                           PasswordEncoder passwordEncoder,
                           EnvioEmail envioEmail,
                           @Value("${app.url-publica}") String urlPublica) {
        this.usuarioRepository = usuarioRepository;
        this.instituicaoRepository = instituicaoRepository;
        this.confirmacaoRepository = confirmacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.envioEmail = envioEmail;
        this.urlPublica = urlPublica;
    }

    @Transactional
    public void cadastrarAutonomo(CadastroAutonomoRequestDTO pedido) {
        String email = Usuario.normalizarEmail(pedido.email());
        String nome = pedido.nome().trim();
        String senhaHash = passwordEncoder.encode(pedido.senha());

        Optional<Usuario> existente = usuarioRepository.findByEmail(email);
        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            if (usuario.isAtivo()) {
                avisarQueJaTemConta(usuario);
            } else {
                usuario.atualizarCadastroPendente(nome, senhaHash);
                enviarConfirmacao(usuario);
            }
            return;
        }

        String nomeEspaco = pedido.nomeEspaco() == null || pedido.nomeEspaco().isBlank()
                ? "Aulas de " + nome
                : pedido.nomeEspaco().trim();
        Instituicao espaco = instituicaoRepository.save(new Instituicao(nomeEspaco, TipoInstituicao.AUTONOMO));
        Usuario professor = new Usuario(nome, email, senhaHash, Papel.PROFESSOR_AUTONOMO, espaco, null);
        professor.aguardarConfirmacaoDeEmail();
        usuarioRepository.save(professor);
        enviarConfirmacao(professor);
    }

    /** Ativa a conta. Link inexistente, já usado ou vencido: mesma resposta 404. */
    @Transactional
    public void confirmar(String codigo) {
        LocalDateTime agora = LocalDateTime.now();
        ConfirmacaoEmail confirmacao = confirmacaoRepository.findByCodigoHash(CodigoConvite.hash(codigo))
                .filter(c -> c.isPendente(agora))
                .orElseThrow(() -> new NaoEncontradoException("Link de confirmação inválido ou expirado."));
        confirmacao.confirmar(agora);
    }

    private void enviarConfirmacao(Usuario usuario) {
        LocalDateTime agora = LocalDateTime.now();
        String codigo = CodigoConvite.gerar();
        confirmacaoRepository.save(new ConfirmacaoEmail(usuario, CodigoConvite.hash(codigo), agora,
                agora.plus(VALIDADE_CONFIRMACAO)));

        envioEmail.enviar(usuario.getEmail(), "Confirme seu e-mail - Notas Escolares", """
                Olá, %s!

                Para ativar sua conta de professor, abra o link abaixo:
                %s/confirmar.html#%s

                O link vale por 24 horas. Se não foi você que se cadastrou, ignore este e-mail.
                """.formatted(usuario.getNome(), urlPublica, codigo));
    }

    private void avisarQueJaTemConta(Usuario usuario) {
        envioEmail.enviar(usuario.getEmail(), "Você já tem uma conta - Notas Escolares", """
                Olá, %s!

                Alguém tentou criar uma conta com este e-mail, mas você já tem uma.
                Para entrar, acesse: %s

                Se não foi você, pode ignorar este e-mail: nada foi alterado na sua conta.
                """.formatted(usuario.getNome(), urlPublica));
    }

}
