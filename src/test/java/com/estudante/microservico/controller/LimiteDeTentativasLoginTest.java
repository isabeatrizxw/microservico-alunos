package com.estudante.microservico.controller;

import com.estudante.microservico.model.Instituicao;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.InstituicaoRepository;
import com.estudante.microservico.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Limite de tentativas com valores baixos (o perfil de teste normal usa valores altos).
 * Cada teste usa e-mails próprios: a contagem fica na memória e vale para o contexto inteiro.
 */
@SpringBootTest(properties = {"app.limite.login-por-email=3", "app.limite.cadastro-por-ip=2"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LimiteDeTentativasLoginTest {

    private static final String SENHA = "senha-certa-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void senhasErradasDemaisBloqueiamOEmailMesmoComASenhaCerta() throws Exception {
        criarProfessor("bloqueado@exemplo.com");
        criarProfessor("livre@exemplo.com");

        for (int i = 0; i < 3; i++) {
            login("bloqueado@exemplo.com", "senha-errada-" + i).andExpect(status().isUnauthorized());
        }

        login("bloqueado@exemplo.com", SENHA)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.erro").value("Muitas tentativas. Aguarde alguns minutos e tente de novo."));
        login("livre@exemplo.com", SENHA).andExpect(status().isOk());
    }

    @Test
    void loginCertoZeraAsFalhasDoEmail() throws Exception {
        criarProfessor("distraido@exemplo.com");

        login("distraido@exemplo.com", "senha-errada-1").andExpect(status().isUnauthorized());
        login("distraido@exemplo.com", "senha-errada-2").andExpect(status().isUnauthorized());
        login("distraido@exemplo.com", SENHA).andExpect(status().isOk());

        login("distraido@exemplo.com", "senha-errada-3").andExpect(status().isUnauthorized());
        login("distraido@exemplo.com", "senha-errada-4").andExpect(status().isUnauthorized());
        login("distraido@exemplo.com", SENHA).andExpect(status().isOk());
    }

    @Test
    void cadastrosDemaisDoMesmoIpSaoBloqueados() throws Exception {
        cadastrar("um@exemplo.com").andExpect(status().isAccepted());
        cadastrar("dois@exemplo.com").andExpect(status().isAccepted());

        cadastrar("tres@exemplo.com").andExpect(status().isTooManyRequests());
    }

    private void criarProfessor(String email) {
        Instituicao escola = instituicaoRepository.save(new Instituicao("Escola " + email));
        usuarioRepository.save(new Usuario("Professor", email, passwordEncoder.encode(SENHA),
                Papel.PROFESSOR, escola, null));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"senha\": \"" + senha + "\"}"));
    }

    private ResultActions cadastrar(String email) throws Exception {
        return mockMvc.perform(post("/api/cadastro/autonomo").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"Professor\", \"email\": \"" + email + "\", \"senha\": \"" + SENHA + "\"}"));
    }

}
