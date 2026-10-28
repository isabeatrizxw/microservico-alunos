package com.estudante.microservico.controller;

import com.estudante.microservico.model.Convite;
import com.estudante.microservico.model.Instituicao;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Turma;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.ConviteRepository;
import com.estudante.microservico.repository.InstituicaoRepository;
import com.estudante.microservico.repository.TurmaRepository;
import com.estudante.microservico.repository.UsuarioRepository;
import com.estudante.microservico.security.CodigoConvite;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Convites: criação pelo admin da escola, aceite público e todas as regras de segurança. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConviteTest {

    private static final String SENHA = "senha-de-teste-123";
    private static final String NOVA_SENHA = "senha-nova-456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ConviteRepository conviteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Instituicao escola;
    private Turma turma;
    private Turma turmaDeOutraEscola;
    private Usuario admin;

    @BeforeEach
    void cadastrarEscolas() {
        escola = instituicaoRepository.save(new Instituicao("Escola Exemplo"));
        turma = turmaRepository.save(new Turma("1º A", escola));
        admin = criarUsuario("secretaria@exemplo.com", Papel.ADMIN_ESCOLA, escola);
        criarUsuario("prof@exemplo.com", Papel.PROFESSOR, escola);

        Instituicao outraEscola = instituicaoRepository.save(new Instituicao("Outra Escola"));
        turmaDeOutraEscola = turmaRepository.save(new Turma("9º B", outraEscola));
        criarUsuario("secretaria@outra.com", Papel.ADMIN_ESCOLA, outraEscola);
    }

    @Test
    void professorConvidadoCriaContaEEntraComOPapelDoConvite() throws Exception {
        String codigo = convidar(login("secretaria@exemplo.com"), "Novo.Prof@Exemplo.com", "PROFESSOR", null);

        mockMvc.perform(get("/api/convites/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("novo.prof@exemplo.com"))
                .andExpect(jsonPath("$.papel").value("PROFESSOR"))
                .andExpect(jsonPath("$.instituicaoNome").value("Escola Exemplo"));

        aceitar(codigo).andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais("novo.prof@exemplo.com", NOVA_SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("PROFESSOR"))
                .andExpect(jsonPath("$.instituicaoId").value(escola.getId()));
    }

    @Test
    void conviteSoPodeSerUsadoUmaVez() throws Exception {
        String codigo = convidar(login("secretaria@exemplo.com"), "novo@exemplo.com", "PROFESSOR", null);
        aceitar(codigo).andExpect(status().isCreated());

        mockMvc.perform(get("/api/convites/" + codigo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Convite inválido ou expirado."));
        aceitar(codigo).andExpect(status().isNotFound());
    }

    @Test
    void alunoConvidadoEntraNaTurmaDoConvite() throws Exception {
        String codigo = convidar(login("secretaria@exemplo.com"), "aluno@exemplo.com", "ALUNO", turma.getId());

        aceitar(codigo).andExpect(status().isCreated());

        Usuario aluno = usuarioRepository.findByEmail("aluno@exemplo.com").orElseThrow();
        assertEquals(Papel.ALUNO, aluno.getPapel());
        assertEquals(turma.getId(), aluno.getTurma().getId());
    }

    @Test
    void validaTurmaConformeOPapel() throws Exception {
        MockHttpSession sessao = login("secretaria@exemplo.com");

        criarConvite(sessao, "a@exemplo.com", "ALUNO", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Informe a turma do aluno."));
        criarConvite(sessao, "p@exemplo.com", "PROFESSOR", turma.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Convite de professor não tem turma."));
        criarConvite(sessao, "x@exemplo.com", "ADMIN_ESCOLA", null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void naoConvidaParaTurmaDeOutraEscola() throws Exception {
        criarConvite(login("secretaria@exemplo.com"), "aluno@exemplo.com", "ALUNO", turmaDeOutraEscola.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Turma não encontrada."));
    }

    @Test
    void naoConvidaEmailQueJaTemContaOuConvitePendente() throws Exception {
        MockHttpSession sessao = login("secretaria@exemplo.com");

        criarConvite(sessao, "prof@exemplo.com", "PROFESSOR", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Já existe uma conta com este e-mail."));

        convidar(sessao, "novo@exemplo.com", "PROFESSOR", null);
        criarConvite(sessao, "NOVO@exemplo.com", "PROFESSOR", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Já existe um convite pendente para este e-mail."));
    }

    @Test
    void listaSoConvitesPendentesDaPropriaEscolaSemOCodigo() throws Exception {
        MockHttpSession sessao = login("secretaria@exemplo.com");
        convidar(sessao, "um@exemplo.com", "PROFESSOR", null);
        String usado = convidar(sessao, "dois@exemplo.com", "PROFESSOR", null);
        aceitar(usado).andExpect(status().isCreated());
        convidar(login("secretaria@outra.com"), "tres@outra.com", "PROFESSOR", null);

        mockMvc.perform(get("/api/escola/convites").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("um@exemplo.com"))
                .andExpect(jsonPath("$[0].codigo").doesNotExist());
    }

    @Test
    void conviteRevogadoDeixaDeFuncionar() throws Exception {
        MockHttpSession sessao = login("secretaria@exemplo.com");
        String resposta = criarConvite(sessao, "novo@exemplo.com", "PROFESSOR", null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(resposta, "$.id");
        String codigo = JsonPath.read(resposta, "$.codigo");

        mockMvc.perform(delete("/api/escola/convites/" + id).session(sessao).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/convites/" + codigo)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/escola/convites/" + id).session(sessao).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void adminNaoRevogaConviteDeOutraEscola() throws Exception {
        String resposta = criarConvite(login("secretaria@outra.com"), "novo@outra.com", "PROFESSOR", null)
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(resposta, "$.id");

        mockMvc.perform(delete("/api/escola/convites/" + id).session(login("secretaria@exemplo.com")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void conviteExpiradoNaoFunciona() throws Exception {
        String codigo = CodigoConvite.gerar();
        LocalDateTime oitoDiasAtras = LocalDateTime.now().minusDays(8);
        conviteRepository.save(new Convite(CodigoConvite.hash(codigo), "atrasado@exemplo.com", Papel.PROFESSOR,
                escola, null, admin, oitoDiasAtras, oitoDiasAtras.plusDays(7)));

        mockMvc.perform(get("/api/convites/" + codigo)).andExpect(status().isNotFound());
        aceitar(codigo).andExpect(status().isNotFound());
    }

    @Test
    void codigoInventadoNaoFunciona() throws Exception {
        mockMvc.perform(get("/api/convites/" + CodigoConvite.gerar())).andExpect(status().isNotFound());
    }

    @Test
    void somenteAdminDaEscolaCriaConvites() throws Exception {
        criarConvite(login("prof@exemplo.com"), "novo@exemplo.com", "PROFESSOR", null)
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/escola/convites").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"novo@exemplo.com\", \"papel\": \"PROFESSOR\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aceitarExigeCsrfESenhaForte() throws Exception {
        String codigo = convidar(login("secretaria@exemplo.com"), "novo@exemplo.com", "PROFESSOR", null);

        mockMvc.perform(post("/api/convites/" + codigo + "/aceitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Novo\", \"senha\": \"" + NOVA_SENHA + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/convites/" + codigo + "/aceitar").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Novo\", \"senha\": \"curta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.senha").value("A senha precisa ter entre 8 e 72 caracteres."));
    }

    private ResultActions criarConvite(MockHttpSession sessao, String email, String papel, Long turmaId)
            throws Exception {
        String turmaJson = turmaId == null ? "" : ", \"turmaId\": " + turmaId;
        return mockMvc.perform(post("/api/escola/convites").session(sessao).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"papel\": \"" + papel + "\"" + turmaJson + "}"));
    }

    private String convidar(MockHttpSession sessao, String email, String papel, Long turmaId) throws Exception {
        String resposta = criarConvite(sessao, email, papel, turmaId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").exists())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.codigo");
    }

    private ResultActions aceitar(String codigo) throws Exception {
        return mockMvc.perform(post("/api/convites/" + codigo + "/aceitar").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"Pessoa Convidada\", \"senha\": \"" + NOVA_SENHA + "\"}"));
    }

    private Usuario criarUsuario(String email, Papel papel, Instituicao instituicao) {
        return usuarioRepository.save(new Usuario("Usuário " + papel, email, passwordEncoder.encode(SENHA),
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
