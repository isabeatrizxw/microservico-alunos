package com.estudante.microservico.controller;

import com.estudante.microservico.model.Disciplina;
import com.estudante.microservico.model.Instituicao;
import com.estudante.microservico.model.Nota;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Turma;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.DisciplinaRepository;
import com.estudante.microservico.repository.InstituicaoRepository;
import com.estudante.microservico.repository.NotaRepository;
import com.estudante.microservico.repository.TurmaRepository;
import com.estudante.microservico.repository.UsuarioRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Gestão de pessoas pela escola: alunos por turma, professores, aulas atribuídas, renomear e excluir. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PessoasTest {

    private static final String SENHA = "senha-de-teste-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private DisciplinaRepository disciplinaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotaRepository notaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Turma turmaA;
    private Turma turmaB;
    private Disciplina matematica;
    private Usuario admin;
    private Usuario prof;
    private Usuario ana;
    private Turma turmaDeOutraEscola;

    @BeforeEach
    void cadastrarEscola() {
        Instituicao escola = instituicaoRepository.save(new Instituicao("Escola Exemplo"));
        turmaA = turmaRepository.save(new Turma("1º A", escola));
        turmaB = turmaRepository.save(new Turma("1º B", escola));
        matematica = disciplinaRepository.save(new Disciplina("Matemática", escola));
        admin = criarUsuario("Secretaria", "secretaria@exemplo.com", Papel.ADMIN_ESCOLA, escola, null);
        prof = criarUsuario("Prof. Carlos", "prof@exemplo.com", Papel.PROFESSOR, escola, null);
        ana = criarUsuario("Ana", "ana@exemplo.com", Papel.ALUNO, escola, turmaA);
        criarUsuario("Bruno", "bruno@exemplo.com", Papel.ALUNO, escola, turmaB);

        Instituicao outraEscola = instituicaoRepository.save(new Instituicao("Outra Escola"));
        turmaDeOutraEscola = turmaRepository.save(new Turma("9º C", outraEscola));
        criarUsuario("Secretaria Outra", "secretaria@outra.com", Papel.ADMIN_ESCOLA, outraEscola, null);
    }

    @Test
    void adminVeAlunosComTurmaEProfessoresComAulas() throws Exception {
        MockHttpSession sessao = login("secretaria@exemplo.com");
        atribuir(sessao, prof.getId(), turmaA.getId(), matematica.getId()).andExpect(status().isCreated());

        mockMvc.perform(get("/api/escola/pessoas").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alunos.length()").value(2))
                .andExpect(jsonPath("$.alunos[0].nome").value("Ana"))
                .andExpect(jsonPath("$.alunos[0].turmaNome").value("1º A"))
                .andExpect(jsonPath("$.alunos[0].ativo").value(true))
                .andExpect(jsonPath("$.professores.length()").value(1))
                .andExpect(jsonPath("$.professores[0].atribuicoes[0].turmaNome").value("1º A"))
                .andExpect(jsonPath("$.professores[0].atribuicoes[0].disciplinaNome").value("Matemática"));
    }

    @Test
    void adminMudaAlunoDeTurma() throws Exception {
        MockHttpSession sessao = login("secretaria@exemplo.com");

        mockMvc.perform(put("/api/escola/alunos/" + ana.getId() + "/turma").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"turmaId\": " + turmaB.getId() + "}"))
                .andExpect(status().isNoContent());
        assertThat(usuarioRepository.findById(ana.getId()).orElseThrow().getTurma().getId()).isEqualTo(turmaB.getId());

        mockMvc.perform(put("/api/escola/alunos/" + ana.getId() + "/turma").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"turmaId\": " + turmaDeOutraEscola.getId() + "}"))
                .andExpect(status().isNotFound());
        // Admin de outra escola não mexe nos alunos desta
        mockMvc.perform(put("/api/escola/alunos/" + ana.getId() + "/turma").session(login("secretaria@outra.com"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"turmaId\": " + turmaDeOutraEscola.getId() + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void alunoDesativadoNaoEntraESomeDaListaDoProfessor() throws Exception {
        MockHttpSession sessao = login("secretaria@exemplo.com");
        atribuir(sessao, prof.getId(), turmaA.getId(), matematica.getId());

        definirAtivo(sessao, ana.getId(), false).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"ana@exemplo.com\", \"senha\": \"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.startsWith("Sua conta está inativa.")));
        mockMvc.perform(get("/api/professor/turmas/" + turmaA.getId() + "/alunos").session(login("prof@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        definirAtivo(sessao, ana.getId(), true).andExpect(status().isNoContent());
        login("ana@exemplo.com");
    }

    @Test
    void adminNaoDesativaAdministradores() throws Exception {
        definirAtivo(login("secretaria@exemplo.com"), admin.getId(), false)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Só é possível ativar ou desativar alunos e professores."));
    }

    @Test
    void atribuirERemoverAulaLiberaEBloqueiaOProfessor() throws Exception {
        MockHttpSession sessao = login("secretaria@exemplo.com");
        MockHttpSession professor = login("prof@exemplo.com");
        mockMvc.perform(get("/api/professor/turmas/" + turmaA.getId() + "/alunos").session(professor))
                .andExpect(status().isForbidden());

        String resposta = atribuir(sessao, prof.getId(), turmaA.getId(), matematica.getId())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/professor/turmas/" + turmaA.getId() + "/alunos").session(professor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Ana"));
        atribuir(sessao, prof.getId(), turmaA.getId(), matematica.getId())
                .andExpect(status().isConflict());

        Integer atribuicaoId = JsonPath.read(resposta, "$.id");
        mockMvc.perform(delete("/api/escola/atribuicoes/" + atribuicaoId).session(sessao).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/professor/turmas/" + turmaA.getId() + "/alunos").session(professor))
                .andExpect(status().isForbidden());
    }

    @Test
    void renomeiaEExcluiTurmaSoQuandoVazia() throws Exception {
        MockHttpSession sessao = login("secretaria@exemplo.com");

        mockMvc.perform(put("/api/escola/turmas/" + turmaA.getId()).session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"1º A - Manhã\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("1º A - Manhã"));
        mockMvc.perform(put("/api/escola/turmas/" + turmaA.getId()).session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"1º b\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/escola/turmas/" + turmaA.getId()).session(sessao).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("A turma tem alunos. Mude os alunos de turma antes de excluir."));

        Turma vazia = turmaRepository.save(new Turma("Turma vazia", turmaA.getInstituicao()));
        mockMvc.perform(delete("/api/escola/turmas/" + vazia.getId()).session(sessao).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(turmaRepository.findById(vazia.getId())).isEmpty();
    }

    @Test
    void naoExcluiDisciplinaComNotas() throws Exception {
        MockHttpSession sessao = login("secretaria@exemplo.com");
        notaRepository.save(new Nota(ana, matematica, "Prova 1", new BigDecimal("8"), prof, LocalDateTime.now()));

        mockMvc.perform(delete("/api/escola/disciplinas/" + matematica.getId()).session(sessao).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("A disciplina já tem notas lançadas e não pode ser excluída."));

        Disciplina semUso = disciplinaRepository.save(new Disciplina("Artes", turmaA.getInstituicao()));
        mockMvc.perform(delete("/api/escola/disciplinas/" + semUso.getId()).session(sessao).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void professorNaoGerenciaPessoas() throws Exception {
        MockHttpSession professor = login("prof@exemplo.com");

        mockMvc.perform(get("/api/escola/pessoas").session(professor)).andExpect(status().isForbidden());
        atribuir(professor, prof.getId(), turmaA.getId(), matematica.getId()).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/escola/turmas/" + turmaB.getId()).session(professor).with(csrf()))
                .andExpect(status().isForbidden());
    }

    private ResultActions atribuir(MockHttpSession sessao, Long professorId, Long turmaId, Long disciplinaId)
            throws Exception {
        return mockMvc.perform(post("/api/escola/atribuicoes").session(sessao).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"professorId\": " + professorId + ", \"turmaId\": " + turmaId
                        + ", \"disciplinaId\": " + disciplinaId + "}"));
    }

    private ResultActions definirAtivo(MockHttpSession sessao, Long usuarioId, boolean ativo) throws Exception {
        return mockMvc.perform(put("/api/escola/pessoas/" + usuarioId + "/ativo").session(sessao).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\": " + ativo + "}"));
    }

    private Usuario criarUsuario(String nome, String email, Papel papel, Instituicao instituicao, Turma turma) {
        return usuarioRepository.save(new Usuario(nome, email, passwordEncoder.encode(SENHA), papel, instituicao, turma));
    }

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"senha\": \"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

}
