package com.estudante.microservico.controller;

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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Login, CSRF, permissões por papel e isolamento entre escolas. Cada teste é desfeito ao final. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SegurancaTest {

    private static final String SENHA = "senha-de-teste-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Instituicao escola;

    @BeforeEach
    void cadastrarUsuarios() {
        escola = instituicaoRepository.save(new Instituicao("Escola Exemplo"));
        criarUsuario("dono@exemplo.com", Papel.SUPER_ADMIN, null);
        criarUsuario("secretaria@exemplo.com", Papel.ADMIN_ESCOLA, escola);
        criarUsuario("prof@exemplo.com", Papel.PROFESSOR, escola);
    }

    @Test
    void loginComSenhaCorretaDevolveUsuarioSemExporASenha() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais("  Prof@Exemplo.com ", SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("prof@exemplo.com"))
                .andExpect(jsonPath("$.papel").value("PROFESSOR"))
                .andExpect(jsonPath("$.instituicaoId").value(escola.getId()))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void senhaErradaEEmailInexistenteDevolvemOMesmo401() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais("prof@exemplo.com", "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("E-mail ou senha inválidos."));

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais("ninguem@exemplo.com", SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("E-mail ou senha inválidos."));
    }

    @Test
    void loginSemTokenCsrfEhRecusado() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais("prof@exemplo.com", SENHA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value(
                        "Token de segurança ausente ou expirado. Recarregue a página e tente de novo."));
    }

    @Test
    void paginasEArquivosDaInterfaceAbremSemLogin() throws Exception {
        for (String caminho : new String[] {"/", "/index.html", "/convite.html", "/cadastro.html", "/confirmar.html",
                "/css/app.css", "/js/main.js", "/js/telas/professor.js", "/js/telas/autonomo.js"}) {
            mockMvc.perform(get(caminho)).andExpect(status().isOk());
        }
    }

    @Test
    void euExigeLoginEDevolveQuemEstaLogado() throws Exception {
        mockMvc.perform(get("/api/auth/eu"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Faça login para continuar."));

        mockMvc.perform(get("/api/auth/eu").session(login("prof@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("prof@exemplo.com"));
    }

    @Test
    void logoutEncerraASessao() throws Exception {
        MockHttpSession sessao = login("prof@exemplo.com");

        mockMvc.perform(post("/api/auth/logout").session(sessao).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/eu").session(sessao))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutJaEntregaNovoTokenCsrfParaOProximoLogin() throws Exception {
        MvcResult logout = mockMvc.perform(post("/api/auth/logout").session(login("prof@exemplo.com")).with(csrf()))
                .andExpect(status().isNoContent())
                .andReturn();

        // O último Set-Cookie do XSRF-TOKEN é o que vale no navegador: precisa ter valor
        String ultimoCookieCsrf = logout.getResponse().getHeaders("Set-Cookie").stream()
                .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
                .reduce((primeiro, segundo) -> segundo)
                .orElseThrow();
        assertFalse(ultimoCookieCsrf.startsWith("XSRF-TOKEN=;"));
    }

    @Test
    void superAdminCriaEscolaEOAdminDelaConsegueEntrar() throws Exception {
        String novaEscola = """
                {"nome": "Colégio Novo", "adminNome": "Coordenação",
                 "adminEmail": "coord@colegionovo.com", "adminSenha": "senha-forte-456"}
                """;

        mockMvc.perform(post("/api/admin/instituicoes").session(login("dono@exemplo.com")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(novaEscola))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Colégio Novo"))
                .andExpect(jsonPath("$.criadoEm").exists());

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais("coord@colegionovo.com", "senha-forte-456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMIN_ESCOLA"));
    }

    @Test
    void naoCriaEscolaComEmailDeAdminJaCadastrado() throws Exception {
        String escolaRepetida = """
                {"nome": "Outra", "adminNome": "Alguém",
                 "adminEmail": "PROF@exemplo.com", "adminSenha": "senha-forte-456"}
                """;

        mockMvc.perform(post("/api/admin/instituicoes").session(login("dono@exemplo.com")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(escolaRepetida))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Já existe um usuário com este e-mail."));
    }

    @Test
    void somenteSuperAdminAcessaAreaDeInstituicoes() throws Exception {
        mockMvc.perform(get("/api/admin/instituicoes").session(login("secretaria@exemplo.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Você não tem permissão para esta ação."));

        mockMvc.perform(get("/api/admin/instituicoes").session(login("prof@exemplo.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void somenteAdminDaEscolaCriaTurmas() throws Exception {
        mockMvc.perform(post("/api/escola/turmas").session(login("prof@exemplo.com")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"1º A\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/escola/turmas").session(login("dono@exemplo.com")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"1º A\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSoVeTurmasDaPropriaEscola() throws Exception {
        Instituicao outraEscola = instituicaoRepository.save(new Instituicao("Outra Escola"));
        criarUsuario("secretaria@outra.com", Papel.ADMIN_ESCOLA, outraEscola);

        mockMvc.perform(post("/api/escola/turmas").session(login("secretaria@exemplo.com")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"1º A\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/escola/turmas").session(login("secretaria@outra.com")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"9º B\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/escola/turmas").session(login("secretaria@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("1º A"));
    }

    @Test
    void naoCriaDuasTurmasComOMesmoNomeNaMesmaEscola() throws Exception {
        MockHttpSession sessao = login("secretaria@exemplo.com");
        mockMvc.perform(post("/api/escola/turmas").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"1º A\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/escola/turmas").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \" 1º a \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Já existe uma turma com este nome."));
    }

    private void criarUsuario(String email, Papel papel, Instituicao instituicao) {
        usuarioRepository.save(new Usuario("Usuário " + papel, email, passwordEncoder.encode(SENHA),
                papel, instituicao, null));
    }

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private static String credenciais(String email, String senha) {
        return "{\"email\": \"" + email + "\", \"senha\": \"" + senha + "\"}";
    }

}
