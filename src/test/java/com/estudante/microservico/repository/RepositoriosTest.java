package com.estudante.microservico.repository;

import com.estudante.microservico.model.Instituicao;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Turma;
import com.estudante.microservico.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Roda contra o H2 de teste com o esquema criado pelas migrações do Flyway,
 * garantindo que entidades, migrações e constraints estão de acordo.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RepositoriosTest {

    private static final String SENHA_HASH = "$2a$10$hashDeTesteNaoEUmaSenhaReal000000000000000000000000";

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void salvaInstituicaoTurmaEAlunoEEncontraAlunosDaTurma() {
        Instituicao escola = instituicaoRepository.save(new Instituicao("Escola Exemplo"));
        Turma turmaA = turmaRepository.save(new Turma("1º A", escola));
        turmaRepository.save(new Turma("1º B", escola));
        usuarioRepository.save(new Usuario("Bruno", "bruno@exemplo.com", SENHA_HASH, Papel.ALUNO, escola, turmaA));
        usuarioRepository.save(new Usuario("Ana", "ana@exemplo.com", SENHA_HASH, Papel.ALUNO, escola, turmaA));

        List<Usuario> alunos = usuarioRepository.findByTurmaIdOrderByNome(turmaA.getId());

        assertThat(alunos).extracting(Usuario::getNome).containsExactly("Ana", "Bruno");
        assertThat(turmaRepository.findByInstituicaoIdOrderByNome(escola.getId()))
                .extracting(Turma::getNome).containsExactly("1º A", "1º B");
    }

    @Test
    void encontraUsuarioPeloEmailNormalizado() {
        Instituicao escola = instituicaoRepository.save(new Instituicao("Escola Exemplo"));
        usuarioRepository.save(new Usuario("Prof. Carla", "  Carla@Exemplo.COM ", SENHA_HASH, Papel.PROFESSOR, escola, null));

        assertThat(usuarioRepository.findByEmail(Usuario.normalizarEmail("CARLA@exemplo.com"))).isPresent();
        assertThat(usuarioRepository.existsByEmail("carla@exemplo.com")).isTrue();
    }

    @Test
    void naoPermiteDoisUsuariosComOMesmoEmail() {
        Instituicao escola = instituicaoRepository.save(new Instituicao("Escola Exemplo"));
        usuarioRepository.saveAndFlush(new Usuario("Ana", "ana@exemplo.com", SENHA_HASH, Papel.PROFESSOR, escola, null));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(
                new Usuario("Outra Ana", "ANA@exemplo.com", SENHA_HASH, Papel.ALUNO, escola, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRejeitaAlunoSemInstituicaoMesmoSemPassarPelaEntidade() {
        // Garante que a regra também está no banco (CHECK), não só no construtor da entidade
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO usuario (nome, email, senha_hash, papel, criado_em) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                "Sem Escola", "sem@exemplo.com", SENHA_HASH, "ALUNO"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
