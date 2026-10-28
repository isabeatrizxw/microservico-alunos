package com.estudante.microservico.service;

import com.estudante.microservico.email.EnvioEmail;
import com.estudante.microservico.exception.NaoEncontradoException;
import com.estudante.microservico.model.RedefinicaoSenha;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.RedefinicaoSenhaRepository;
import com.estudante.microservico.repository.UsuarioRepository;
import com.estudante.microservico.security.CodigoConvite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * "Esqueci minha senha": envia por e-mail um link de uso único, válido por 1 hora.
 * O pedido responde sempre igual (o controller cuida disso): só o dono do e-mail fica sabendo se há conta.
 */
@Service
public class SenhaService {

    static final Duration VALIDADE = Duration.ofHours(1);

    private final UsuarioRepository usuarioRepository;
    private final RedefinicaoSenhaRepository redefinicaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnvioEmail envioEmail;
    private final String urlPublica;

    public SenhaService(UsuarioRepository usuarioRepository,
                        RedefinicaoSenhaRepository redefinicaoRepository,
                        PasswordEncoder passwordEncoder,
                        EnvioEmail envioEmail,
                        @Value("${app.url-publica}") String urlPublica) {
        this.usuarioRepository = usuarioRepository;
        this.redefinicaoRepository = redefinicaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.envioEmail = envioEmail;
        this.urlPublica = urlPublica;
    }

    /** Conta inexistente ou inativa: não envia nada, sem avisar quem pediu. */
    @Transactional
    public void solicitar(String email) {
        usuarioRepository.findByEmail(Usuario.normalizarEmail(email))
                .filter(Usuario::isAtivo)
                .ifPresent(this::enviarLink);
    }

    /** Troca a senha e devolve o e-mail da conta. Link inexistente, usado ou vencido: 404. */
    @Transactional
    public String redefinir(String codigo, String novaSenha) {
        LocalDateTime agora = LocalDateTime.now();
        RedefinicaoSenha redefinicao = redefinicaoRepository.findByCodigoHash(CodigoConvite.hash(codigo))
                .filter(r -> r.isPendente(agora) && r.getUsuario().isAtivo())
                .orElseThrow(() -> new NaoEncontradoException("Link de redefinição inválido ou expirado."));
        redefinicao.usar(agora, passwordEncoder.encode(novaSenha));
        return redefinicao.getUsuario().getEmail();
    }

    private void enviarLink(Usuario usuario) {
        LocalDateTime agora = LocalDateTime.now();
        String codigo = CodigoConvite.gerar();
        redefinicaoRepository.save(new RedefinicaoSenha(usuario, CodigoConvite.hash(codigo), agora, agora.plus(VALIDADE)));

        envioEmail.enviar(usuario.getEmail(), "Redefinir senha - Notas Escolares", """
                Olá, %s!

                Recebemos um pedido para criar uma nova senha para sua conta.
                Para continuar, abra o link abaixo:
                %s/redefinir-senha.html#%s

                O link vale por 1 hora. Se não foi você que pediu, ignore este e-mail: sua senha continua a mesma.
                """.formatted(usuario.getNome(), urlPublica, codigo));
    }

}
