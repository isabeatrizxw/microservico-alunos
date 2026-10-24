package com.estudante.microservico.controller;

import com.estudante.microservico.email.EmailsCapturados;
import com.estudante.microservico.email.EmailsDeTeste;
import com.estudante.microservico.model.Instituicao;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.InstituicaoRepository;
import com.estudante.microservico.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** "Esqueci minha senha": link por e-mail, uso único e resposta que não revela quem tem conta. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(EmailsDeTeste.class)
class RedefinicaoSenhaTest {

    private static final String SENHA_ANTIGA = "senha-antiga-123";
    private static final String SENHA_NOVA = "senha-nova-456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailsCapturados emails;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario inativo;

    @BeforeEach
    void cadastrar() {
        emails.limpar();
        Instituicao escola = instituicaoRepository.save(new Instituicao("Escola Exemplo"));
        usuarioRepository.save(new Usuario("Prof. Ana", "ana@exemplo.com", passwordEncoder.encode(SENHA_ANTIGA),
                Papel.PROFESSOR, escola, null));
        inativo = new Usuario("Prof. Bia", "bia@exemplo.com", passwordEncoder.encode(SENHA_ANTIGA),
                Papel.PROFESSOR, escola, null);
        inativo.desativar();
        usuarioRepository.save(inativo);
    }

    @Test
    void trocaASenhaPeloLinkEnviadoPorEmail() throws Exception {
        esqueci("Ana@Exemplo.com").andExpect(status().isAccepted());
        assertThat(emails.ultimoPara("ana@exemplo.com").texto()).contains("/redefinir-senha.html#");

        redefinir(emails.codigoDoUltimoLinkPara("ana@exemplo.com"), SENHA_NOVA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Senha alterada! Entre com a nova senha."));

        login("ana@exemplo.com", SENHA_ANTIGA).andExpect(status().isUnauthorized());
        login("ana@exemplo.com", SENHA_NOVA).andExpect(status().isOk());
    }

    @Test
    void linkSoFuncionaUmaVez() throws Exception {
        esqueci("ana@exemplo.com");
        String codigo = emails.codigoDoUltimoLinkPara("ana@exemplo.com");

        redefinir(codigo, SENHA_NOVA).andExpect(status().isOk());
        redefinir(codigo, "outra-senha-789")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Link de redefinição inválido ou expirado."));
        redefinir("codigo-inventado", SENHA_NOVA).andExpect(status().isNotFound());
    }

    @Test
    void emailSemContaOuContaInativaRecebemAMesmaRespostaSemEmail() throws Exception {
        String mensagemComConta = esqueci("ana@exemplo.com")
                .andReturn().getResponse().getContentAsString();

        String mensagemSemConta = esqueci("ninguem@exemplo.com")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        esqueci("bia@exemplo.com").andExpect(status().isAccepted());

        assertThat(mensagemSemConta).isEqualTo(mensagemComConta);
        assertThat(emails.enviadosPara("ninguem@exemplo.com")).isEmpty();
        assertThat(emails.enviadosPara(inativo.getEmail())).isEmpty();
    }

    @Test
    void validaASenhaNova() throws Exception {
        esqueci("ana@exemplo.com");

        redefinir(emails.codigoDoUltimoLinkPara("ana@exemplo.com"), "curta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.senha").value("A senha precisa ter entre 8 e 72 caracteres."));
    }

    private ResultActions esqueci(String email) throws Exception {
        return mockMvc.perform(post("/api/senha/esqueci").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\": \"" + email + "\"}"));
    }

    private ResultActions redefinir(String codigo, String senha) throws Exception {
        return mockMvc.perform(post("/api/senha/redefinir").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\": \"" + codigo + "\", \"senha\": \"" + senha + "\"}"));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"senha\": \"" + senha + "\"}"));
    }

}
