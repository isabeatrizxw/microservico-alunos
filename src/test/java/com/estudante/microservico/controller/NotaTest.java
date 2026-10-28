package com.estudante.microservico.controller;

import com.estudante.microservico.model.Atribuicao;
import com.estudante.microservico.model.Disciplina;
import com.estudante.microservico.repository.AtribuicaoRepository;
import com.estudante.microservico.model.Instituicao;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Turma;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.DisciplinaRepository;
import com.estudante.microservico.repository.InstituicaoRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Notas, boletim e média da turma: regras de cálculo, autoria e isolamento entre escolas. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotaTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AtribuicaoRepository atribuicaoRepository;

    private Turma turma;
    private Disciplina matematica;
    private Usuario ana;
    private Usuario bruno;
    private Usuario prof;
    private Usuario alunoDeOutraEscola;
    private Disciplina disciplinaDeOutraEscola;
    private Turma turmaDeOutraEscola;

    @BeforeEach
    void cadastrarEscolas() {
        Instituicao escola = instituicaoRepository.save(new Instituicao("Escola Exemplo"));
        turma = turmaRepository.save(new Turma("1º A", escola));
        matematica = disciplinaRepository.save(new Disciplina("Matemática", escola));
        Disciplina portugues = disciplinaRepository.save(new Disciplina("Português", escola));
        criarUsuario("Secretaria", "secretaria@exemplo.com", Papel.ADMIN_ESCOLA, escola, null);
        prof = criarUsuario("Prof. Carlos", "prof@exemplo.com", Papel.PROFESSOR, escola, null);
        // O Prof. Carlos dá Matemática e Português no 1º A; a Prof. Dora não tem aula atribuída
        atribuicaoRepository.save(new Atribuicao(prof, turma, matematica));
        atribuicaoRepository.save(new Atribuicao(prof, turma, portugues));
        criarUsuario("Prof. Dora", "prof2@exemplo.com", Papel.PROFESSOR, escola, null);
        ana = criarUsuario("Ana", "ana@exemplo.com", Papel.ALUNO, escola, turma);
        bruno = criarUsuario("Bruno", "bruno@exemplo.com", Papel.ALUNO, escola, turma);
        criarUsuario("Carla", "carla@exemplo.com", Papel.ALUNO, escola, turma);

        Instituicao outraEscola = instituicaoRepository.save(new Instituicao("Outra Escola"));
        turmaDeOutraEscola = turmaRepository.save(new Turma("9º B", outraEscola));
        disciplinaDeOutraEscola = disciplinaRepository.save(new Disciplina("História", outraEscola));
        alunoDeOutraEscola = criarUsuario("Davi", "davi@outra.com", Papel.ALUNO, outraEscola, turmaDeOutraEscola);
        criarUsuario("Prof. Eva", "prof@outra.com", Papel.PROFESSOR, outraEscola, null);
    }

    @Test
    void professorLancaNotasEAlunoVeOBoletimComMediaPorDisciplina() throws Exception {
        MockHttpSession professor = login("prof@exemplo.com");
        lancar(professor, ana.getId(), matematica.getId(), "Prova 1", "8").andExpect(status().isCreated())
                .andExpect(jsonPath("$.lancadaPorNome").value("Prof. Carlos"))
                .andExpect(jsonPath("$.lancadaEm").exists());
        lancar(professor, ana.getId(), matematica.getId(), "Prova 2", "9").andExpect(status().isCreated());

        mockMvc.perform(get("/api/aluno/boletim").session(login("ana@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alunoNome").value("Ana"))
                .andExpect(jsonPath("$.turmaNome").value("1º A"))
                .andExpect(jsonPath("$.calculadoEm").exists())
                .andExpect(jsonPath("$.disciplinas.length()").value(2))
                .andExpect(jsonPath("$.disciplinas[0].disciplinaNome").value("Matemática"))
                .andExpect(jsonPath("$.disciplinas[0].notas.length()").value(2))
                .andExpect(jsonPath("$.disciplinas[0].media").value(8.5))
                .andExpect(jsonPath("$.disciplinas[0].situacao").value("APROVADO"))
                .andExpect(jsonPath("$.disciplinas[1].disciplinaNome").value("Português"))
                .andExpect(jsonPath("$.disciplinas[1].situacao").value("SEM_NOTAS"));
    }

    @Test
    void alunoSoVeAsPropriasNotas() throws Exception {
        MockHttpSession professor = login("prof@exemplo.com");
        lancar(professor, bruno.getId(), matematica.getId(), "Prova 1", "3").andExpect(status().isCreated());

        mockMvc.perform(get("/api/aluno/boletim").session(login("ana@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disciplinas[0].notas.length()").value(0))
                .andExpect(jsonPath("$.disciplinas[0].situacao").value("SEM_NOTAS"));
    }

    @Test
    void mediaDaTurmaEhAMediaDasMediasSemContarQuemNaoTemNotas() throws Exception {
        MockHttpSession professor = login("prof@exemplo.com");
        lancar(professor, ana.getId(), matematica.getId(), "Prova 1", "8");
        lancar(professor, ana.getId(), matematica.getId(), "Prova 2", "9");
        lancar(professor, bruno.getId(), matematica.getId(), "Prova 1", "5");
        lancar(professor, bruno.getId(), matematica.getId(), "Prova 2", "5");

        mockMvc.perform(get("/api/professor/turmas/" + turma.getId() + "/medias")
                        .param("disciplinaId", matematica.getId().toString()).session(professor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turmaNome").value("1º A"))
                .andExpect(jsonPath("$.disciplinaNome").value("Matemática"))
                .andExpect(jsonPath("$.alunos.length()").value(3))
                .andExpect(jsonPath("$.alunos[0].alunoNome").value("Ana"))
                .andExpect(jsonPath("$.alunos[0].media").value(8.5))
                .andExpect(jsonPath("$.alunos[0].situacao").value("APROVADO"))
                .andExpect(jsonPath("$.alunos[1].media").value(5.0))
                .andExpect(jsonPath("$.alunos[1].situacao").value("REPROVADO"))
                .andExpect(jsonPath("$.alunos[2].quantidadeNotas").value(0))
                .andExpect(jsonPath("$.alunos[2].situacao").value("SEM_NOTAS"))
                .andExpect(jsonPath("$.mediaTurma").value(6.75))
                .andExpect(jsonPath("$.aprovados").value(1))
                .andExpect(jsonPath("$.reprovados").value(1))
                .andExpect(jsonPath("$.semNotas").value(1))
                .andExpect(jsonPath("$.calculadoEm").exists());
    }

    @Test
    void somenteOProfessorQueLancouAlteraOuApagaANota() throws Exception {
        MockHttpSession autor = login("prof@exemplo.com");
        MockHttpSession outroProfessor = login("prof2@exemplo.com");
        String resposta = lancar(autor, ana.getId(), matematica.getId(), "Prova 1", "6")
                .andReturn().getResponse().getContentAsString();
        Integer notaId = JsonPath.read(resposta, "$.id");

        alterar(outroProfessor, notaId, "7.5")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Apenas o professor que lançou a nota pode alterá-la ou apagá-la."));
        mockMvc.perform(delete("/api/professor/notas/" + notaId).session(outroProfessor).with(csrf()))
                .andExpect(status().isForbidden());

        alterar(autor, notaId, "7.5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(7.5))
                .andExpect(jsonPath("$.atualizadaEm").exists());
        mockMvc.perform(delete("/api/professor/notas/" + notaId).session(autor).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void naoLancaNotaParaAlunoOuDisciplinaDeOutraEscola() throws Exception {
        MockHttpSession professor = login("prof@exemplo.com");

        lancar(professor, alunoDeOutraEscola.getId(), matematica.getId(), "Prova 1", "8")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Aluno não encontrado."));
        lancar(professor, ana.getId(), disciplinaDeOutraEscola.getId(), "Prova 1", "8")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Disciplina não encontrada."));
        // Um professor não é aluno: não recebe nota
        lancar(professor, prof.getId(), matematica.getId(), "Prova 1", "8")
                .andExpect(status().isNotFound());
    }

    @Test
    void professorNaoVeDadosDeOutraEscola() throws Exception {
        MockHttpSession professor = login("prof@exemplo.com");

        mockMvc.perform(get("/api/professor/turmas/" + turmaDeOutraEscola.getId() + "/alunos").session(professor))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/professor/alunos/" + alunoDeOutraEscola.getId() + "/boletim").session(professor))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/professor/alunos/" + ana.getId() + "/boletim").session(login("prof@outra.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void professorListaAlunosDaTurmaEVeBoletimDeAlunoDaEscola() throws Exception {
        MockHttpSession professor = login("prof@exemplo.com");

        mockMvc.perform(get("/api/professor/turmas/" + turma.getId() + "/alunos").session(professor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[2].nome").value("Carla"));
        mockMvc.perform(get("/api/professor/alunos/" + ana.getId() + "/boletim").session(professor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alunoNome").value("Ana"));
    }

    @Test
    void professorSemAulaAtribuidaNaoAcessaATurma() throws Exception {
        MockHttpSession semAula = login("prof2@exemplo.com");

        mockMvc.perform(get("/api/professor/turmas/" + turma.getId() + "/alunos").session(semAula))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Você não dá aula nesta turma."));
        lancar(semAula, ana.getId(), matematica.getId(), "Prova 1", "8")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Você não dá esta disciplina nesta turma."));
        mockMvc.perform(get("/api/professor/turmas/" + turma.getId() + "/medias")
                        .param("disciplinaId", matematica.getId().toString()).session(semAula))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/professor/alunos/" + ana.getId() + "/boletim").session(semAula))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/professor/atribuicoes").session(semAula))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void professorVeSoAsPropriasAulas() throws Exception {
        mockMvc.perform(get("/api/professor/atribuicoes").session(login("prof@exemplo.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].turmaNome").value("1º A"))
                .andExpect(jsonPath("$[0].disciplinaNome").value("Matemática"));
    }

    @Test
    void validaOValorDaNota() throws Exception {
        MockHttpSession professor = login("prof@exemplo.com");

        lancar(professor, ana.getId(), matematica.getId(), "Prova 1", "10.5")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valor").value("A nota máxima é 10."));
        lancar(professor, ana.getId(), matematica.getId(), "Prova 1", "-1")
                .andExpect(status().isBadRequest());
        lancar(professor, ana.getId(), matematica.getId(), "Prova 1", "7.555")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valor").value("A nota pode ter no máximo 2 casas decimais."));
    }

    @Test
    void cadaPapelSoAcessaSuaArea() throws Exception {
        MockHttpSession aluno = login("ana@exemplo.com");
        MockHttpSession professor = login("prof@exemplo.com");
        MockHttpSession admin = login("secretaria@exemplo.com");

        lancar(aluno, ana.getId(), matematica.getId(), "Auto nota", "10").andExpect(status().isForbidden());
        lancar(admin, ana.getId(), matematica.getId(), "Prova 1", "10").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/professor/turmas/" + turma.getId() + "/alunos").session(aluno))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/aluno/boletim").session(professor)).andExpect(status().isForbidden());

        // Professor lê turmas e disciplinas da escola, mas não cria
        mockMvc.perform(get("/api/escola/turmas").session(professor)).andExpect(status().isOk());
        mockMvc.perform(get("/api/escola/disciplinas").session(professor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(post("/api/escola/disciplinas").session(professor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"Física\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCriaDisciplinaSemRepetirNome() throws Exception {
        MockHttpSession admin = login("secretaria@exemplo.com");

        mockMvc.perform(post("/api/escola/disciplinas").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"Física\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Física"));
        mockMvc.perform(post("/api/escola/disciplinas").session(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \" matemática \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Já existe uma disciplina com este nome."));
    }

    private ResultActions lancar(MockHttpSession sessao, Long alunoId, Long disciplinaId,
                                 String descricao, String valor) throws Exception {
        return mockMvc.perform(post("/api/professor/notas").session(sessao).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"alunoId\": " + alunoId + ", \"disciplinaId\": " + disciplinaId
                        + ", \"descricao\": \"" + descricao + "\", \"valor\": " + valor + "}"));
    }

    private ResultActions alterar(MockHttpSession sessao, Integer notaId, String valor) throws Exception {
        return mockMvc.perform(put("/api/professor/notas/" + notaId).session(sessao).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\": \"Prova 1 (corrigida)\", \"valor\": " + valor + "}"));
    }

    private Usuario criarUsuario(String nome, String email, Papel papel, Instituicao instituicao, Turma turmaDoAluno) {
        return usuarioRepository.save(new Usuario(nome, email, passwordEncoder.encode(SENHA),
                papel, instituicao, turmaDoAluno));
    }

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"senha\": \"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

}
