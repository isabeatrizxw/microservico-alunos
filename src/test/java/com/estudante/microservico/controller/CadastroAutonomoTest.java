package com.estudante.microservico.controller;

import com.estudante.microservico.email.EmailsCapturados;
import com.estudante.microservico.email.EmailsDeTeste;
import com.estudante.microservico.model.Instituicao;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.TipoInstituicao;
import com.estudante.microservico.model.Turma;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.InstituicaoRepository;
import com.estudante.microservico.repository.TurmaRepository;
import com.estudante.microservico.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cadastro público de professor autônomo, confirmação de e-mail e o uso do espaço dele. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(EmailsDeTeste.class)
class CadastroAutonomoTest {

    private static final String SENHA = "senha-autonoma-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailsCapturados emails;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void limparEmails() {
        emails.limpar();
    }

    @Test
    void cadastroCriaContaInativaQueSoEntraDepoisDeConfirmarOEmail() throws Exception {
        cadastrar("Carla", "carla@exemplo.com", SENHA, null)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensagem").exists());
        assertThat(emails.ultimoPara("carla@exemplo.com").texto()).contains("/confirmar.html#");

        // Senha certa: avisa que falta confirmar. Senha errada: mensagem genérica de sempre.
        login("carla@exemplo.com", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.startsWith("Sua conta está inativa.")));
        login("carla@exemplo.com", "senha-errada-1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("E-mail ou senha inválidos."));

        confirmar(emails.codigoDoUltimoLinkPara("carla@exemplo.com")).andExpect(status().isOk());

        login("carla@exemplo.com", SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("PROFESSOR_AUTONOMO"));
        Usuario carla = usuarioRepository.findByEmail("carla@exemplo.com").orElseThrow();
        assertThat(carla.getInstituicao().getTipo()).isEqualTo(TipoInstituicao.AUTONOMO);
        assertThat(carla.getInstituicao().getNome()).isEqualTo("Aulas de Carla");
    }

    @Test
    void linkDeConfirmacaoSoFuncionaUmaVez() throws Exception {
        cadastrar("Carla", "carla@exemplo.com", SENHA, "Reforço da Carla");
        String codigo = emails.codigoDoUltimoLinkPara("carla@exemplo.com");

        confirmar(codigo).andExpect(status().isOk());
        confirmar(codigo)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Link de confirmação inválido ou expirado."));
        confirmar("codigo-inventado").andExpect(status().isNotFound());
    }

    @Test
    void cadastroComEmailJaUsadoNaoRevelaQueAContaExiste() throws Exception {
        Instituicao escola = instituicaoRepository.save(new Instituicao("Escola Exemplo"));
        usuarioRepository.save(new Usuario("Prof. Ana", "ana@exemplo.com", passwordEncoder.encode(SENHA),
                Papel.PROFESSOR, escola, null));
        long instituicoesAntes = instituicaoRepository.count();

        cadastrar("Outra Pessoa", "ana@exemplo.com", "senha-qualquer-1", null)
                .andExpect(status().isAccepted());

        assertThat(instituicaoRepository.count()).isEqualTo(instituicoesAntes);
        assertThat(emails.ultimoPara("ana@exemplo.com").assunto()).contains("Você já tem uma conta");
        // A senha da conta existente não muda
        login("ana@exemplo.com", SENHA).andExpect(status().isOk());
    }

    @Test
    void recadastroDeContaPendenteValeOUltimoPedido() throws Exception {
        cadastrar("Carla", "carla@exemplo.com", "primeira-senha-1", null);
        cadastrar("Carla Souza", "carla@exemplo.com", "segunda-senha-2", null);

        confirmar(emails.codigoDoUltimoLinkPara("carla@exemplo.com")).andExpect(status().isOk());

        login("carla@exemplo.com", "primeira-senha-1").andExpect(status().isUnauthorized());
        login("carla@exemplo.com", "segunda-senha-2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carla Souza"));
    }

    @Test
    void autonomoMontaTurmaConvidaAlunoPorEmailELancaNota() throws Exception {
        MockHttpSession professor = autonomoConfirmado("carla@exemplo.com");

        Integer turmaId = JsonPath.read(postar(professor, "/api/escola/turmas", "{\"nome\": \"Turma de sábado\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        Integer disciplinaId = JsonPath.read(postar(professor, "/api/escola/disciplinas", "{\"nome\": \"Física\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

        postar(professor, "/api/escola/convites", "{\"email\": \"prof@exemplo.com\", \"papel\": \"PROFESSOR\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Professor autônomo convida apenas alunos."));

        postar(professor, "/api/escola/convites",
                "{\"email\": \"aluno@exemplo.com\", \"papel\": \"ALUNO\", \"turmaId\": " + turmaId + "}")
                .andExpect(status().isCreated());
        String codigoConvite = emails.codigoDoUltimoLinkPara("aluno@exemplo.com");
        mockMvc.perform(post("/api/convites/" + codigoConvite + "/aceitar").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Davi\", \"senha\": \"senha-do-davi-1\"}"))
                .andExpect(status().isCreated());

        Long alunoId = usuarioRepository.findByEmail("aluno@exemplo.com").orElseThrow().getId();
        postar(professor, "/api/professor/notas", "{\"alunoId\": " + alunoId + ", \"disciplinaId\": " + disciplinaId
                + ", \"descricao\": \"Lista 1\", \"valor\": 9}")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/professor/turmas/" + turmaId + "/medias")
                        .param("disciplinaId", disciplinaId.toString()).session(professor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alunos[0].alunoNome").value("Davi"))
                .andExpect(jsonPath("$.alunos[0].situacao").value("APROVADO"));
    }

    @Test
    void autonomoNaoVeDadosDeEscolas() throws Exception {
        Instituicao escola = instituicaoRepository.save(new Instituicao("Escola Exemplo"));
        Turma turmaDaEscola = turmaRepository.save(new Turma("1º A", escola));
        MockHttpSession professor = autonomoConfirmado("carla@exemplo.com");

        mockMvc.perform(get("/api/escola/turmas").session(professor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/professor/turmas/" + turmaDaEscola.getId() + "/alunos").session(professor))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/instituicoes").session(professor))
                .andExpect(status().isForbidden());
    }

    @Test
    void validaOsDadosDoCadastro() throws Exception {
        cadastrar("Carla", "carla@exemplo.com", "curta", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.senha").value("A senha precisa ter entre 8 e 72 caracteres."));
        cadastrar("", "nao-e-email", SENHA, null)
                .andExpect(status().isBadRequest());
        assertThat(emails.enviadosPara("carla@exemplo.com")).isEmpty();
    }

    private MockHttpSession autonomoConfirmado(String email) throws Exception {
        cadastrar("Carla", email, SENHA, null).andExpect(status().isAccepted());
        confirmar(emails.codigoDoUltimoLinkPara(email)).andExpect(status().isOk());
        return (MockHttpSession) login(email, SENHA).andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private ResultActions cadastrar(String nome, String email, String senha, String nomeEspaco) throws Exception {
        String espaco = nomeEspaco == null ? "null" : "\"" + nomeEspaco + "\"";
        return mockMvc.perform(post("/api/cadastro/autonomo").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"" + nome + "\", \"email\": \"" + email + "\", \"senha\": \"" + senha
                        + "\", \"nomeEspaco\": " + espaco + "}"));
    }

    private ResultActions confirmar(String codigo) throws Exception {
        return mockMvc.perform(post("/api/cadastro/confirmar").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\": \"" + codigo + "\"}"));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"senha\": \"" + senha + "\"}"));
    }

    private ResultActions postar(MockHttpSession sessao, String rota, String corpo) throws Exception {
        return mockMvc.perform(post(rota).session(sessao).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

}
