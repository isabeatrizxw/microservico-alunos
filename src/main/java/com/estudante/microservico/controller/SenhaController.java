package com.estudante.microservico.controller;

import com.estudante.microservico.dto.EsqueciSenhaRequestDTO;
import com.estudante.microservico.dto.MensagemDTO;
import com.estudante.microservico.dto.RedefinirSenhaRequestDTO;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.security.LimiteDeTentativas;
import com.estudante.microservico.service.SenhaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** "Esqueci minha senha" (público, sem login). */
@RestController
@RequestMapping("/api/senha")
public class SenhaController {

    private static final Duration JANELA = Duration.ofHours(1);

    private final SenhaService senhaService;
    private final LimiteDeTentativas limiteDeTentativas;
    private final LimiteDeTentativas.Regra regraPorIp;
    private final LimiteDeTentativas.Regra regraPorEmail;

    public SenhaController(SenhaService senhaService,
                           LimiteDeTentativas limiteDeTentativas,
                           @Value("${app.limite.redefinicao-por-ip}") int maximoPorIp,
                           @Value("${app.limite.redefinicao-por-email}") int maximoPorEmail) {
        this.senhaService = senhaService;
        this.limiteDeTentativas = limiteDeTentativas;
        this.regraPorIp = new LimiteDeTentativas.Regra(maximoPorIp, JANELA);
        this.regraPorEmail = new LimiteDeTentativas.Regra(maximoPorEmail, JANELA);
    }

    /** Sempre a mesma resposta (202), exista ou não conta com o e-mail. */
    @PostMapping("/esqueci")
    public ResponseEntity<MensagemDTO> esqueci(@Valid @RequestBody EsqueciSenhaRequestDTO pedido,
                                               HttpServletRequest request) {
        limiteDeTentativas.consumir("senha-ip:" + request.getRemoteAddr(), regraPorIp);
        limiteDeTentativas.consumir("senha-email:" + Usuario.normalizarEmail(pedido.email()), regraPorEmail);
        senhaService.solicitar(pedido.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MensagemDTO(
                "Se houver uma conta com este e-mail, enviamos um link para criar uma nova senha. Ele vale 1 hora."));
    }

    @PostMapping("/redefinir")
    public ResponseEntity<MensagemDTO> redefinir(@Valid @RequestBody RedefinirSenhaRequestDTO pedido) {
        String email = senhaService.redefinir(pedido.codigo(), pedido.senha());
        // Senha nova: as tentativas erradas de antes não devem mais bloquear o login
        limiteDeTentativas.limpar("login-email:" + email);
        return ResponseEntity.ok(new MensagemDTO("Senha alterada! Entre com a nova senha."));
    }

}
