package com.estudante.microservico.controller;

import com.estudante.microservico.dto.LoginRequestDTO;
import com.estudante.microservico.dto.UsuarioLogadoDTO;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.security.LimiteDeTentativas;
import com.estudante.microservico.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Login e consulta de quem está logado. O logout ({@code POST /api/auth/logout})
 * é feito pelo próprio Spring Security (ver SegurancaConfig).
 */
@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private static final Duration JANELA_DE_FALHAS = Duration.ofMinutes(15);

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LimiteDeTentativas limiteDeTentativas;
    private final LimiteDeTentativas.Regra regraPorEmail;
    private final LimiteDeTentativas.Regra regraPorIp;

    public AutenticacaoController(AuthenticationManager authenticationManager,
                                  SecurityContextRepository securityContextRepository,
                                  LimiteDeTentativas limiteDeTentativas,
                                  @Value("${app.limite.login-por-email}") int maximoPorEmail,
                                  @Value("${app.limite.login-por-ip}") int maximoPorIp) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.limiteDeTentativas = limiteDeTentativas;
        this.regraPorEmail = new LimiteDeTentativas.Regra(maximoPorEmail, JANELA_DE_FALHAS);
        this.regraPorIp = new LimiteDeTentativas.Regra(maximoPorIp, JANELA_DE_FALHAS);
    }

    /**
     * Confere e-mail e senha e guarda o usuário na sessão. Senha errada ou e-mail inexistente
     * geram a mesma resposta 401, para não revelar quais e-mails estão cadastrados.
     * Senhas erradas demais (por e-mail ou por IP) bloqueiam novas tentativas por 15 minutos (429).
     */
    @PostMapping("/login")
    public ResponseEntity<UsuarioLogadoDTO> login(@Valid @RequestBody LoginRequestDTO login,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        String chaveEmail = "login-email:" + Usuario.normalizarEmail(login.email());
        String chaveIp = "login-ip:" + request.getRemoteAddr();
        limiteDeTentativas.exigirDisponivel(chaveEmail, regraPorEmail);
        limiteDeTentativas.exigirDisponivel(chaveIp, regraPorIp);

        Authentication autenticacao;
        try {
            autenticacao = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(login.email(), login.senha()));
        } catch (BadCredentialsException senhaErrada) {
            limiteDeTentativas.registrar(chaveEmail, regraPorEmail);
            limiteDeTentativas.registrar(chaveIp, regraPorIp);
            throw senhaErrada;
        }
        limiteDeTentativas.limpar(chaveEmail);

        // Troca o id da sessão no login para impedir "session fixation"
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);

        return ResponseEntity.ok(UsuarioLogadoDTO.de((UsuarioAutenticado) autenticacao.getPrincipal()));
    }

    @GetMapping("/eu")
    public ResponseEntity<UsuarioLogadoDTO> usuarioLogado(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(UsuarioLogadoDTO.de(usuario));
    }

}
