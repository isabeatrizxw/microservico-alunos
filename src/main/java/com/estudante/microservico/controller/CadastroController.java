package com.estudante.microservico.controller;

import com.estudante.microservico.dto.CadastroAutonomoRequestDTO;
import com.estudante.microservico.dto.ConfirmarEmailRequestDTO;
import com.estudante.microservico.dto.MensagemDTO;
import com.estudante.microservico.security.LimiteDeTentativas;
import com.estudante.microservico.service.CadastroService;
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

/** Cadastro público (sem login) de professor autônomo e confirmação de e-mail. */
@RestController
@RequestMapping("/api/cadastro")
public class CadastroController {

    private static final Duration JANELA_DE_CADASTRO = Duration.ofHours(1);

    private final CadastroService cadastroService;
    private final LimiteDeTentativas limiteDeTentativas;
    private final LimiteDeTentativas.Regra regraPorIp;

    public CadastroController(CadastroService cadastroService,
                              LimiteDeTentativas limiteDeTentativas,
                              @Value("${app.limite.cadastro-por-ip}") int maximoPorIp) {
        this.cadastroService = cadastroService;
        this.limiteDeTentativas = limiteDeTentativas;
        this.regraPorIp = new LimiteDeTentativas.Regra(maximoPorIp, JANELA_DE_CADASTRO);
    }

    /**
     * Sempre a mesma resposta (202), exista ou não conta com o e-mail: o que muda é o e-mail enviado.
     * Assim o cadastro não serve para descobrir quem usa o sistema.
     */
    @PostMapping("/autonomo")
    public ResponseEntity<MensagemDTO> cadastrarAutonomo(@Valid @RequestBody CadastroAutonomoRequestDTO pedido,
                                                         HttpServletRequest request) {
        limiteDeTentativas.consumir("cadastro-ip:" + request.getRemoteAddr(), regraPorIp);
        cadastroService.cadastrarAutonomo(pedido);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MensagemDTO(
                "Enviamos um link de confirmação para " + pedido.email().trim()
                        + ". Abra o link (vale 24 horas) para ativar sua conta."));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<MensagemDTO> confirmar(@Valid @RequestBody ConfirmarEmailRequestDTO pedido) {
        cadastroService.confirmar(pedido.codigo());
        return ResponseEntity.ok(new MensagemDTO("E-mail confirmado! Você já pode entrar."));
    }

}
