package com.estudante.microservico.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsuarioTest {

    private static final String SENHA_HASH = "hash";

    private final Instituicao escola = new Instituicao("Escola Exemplo");
    private final Turma turma = new Turma("1º A", escola);

    @Test
    void normalizaEmailParaMinusculasSemEspacos() {
        Usuario aluno = new Usuario("Ana", "  Ana@Exemplo.COM ", SENHA_HASH, Papel.ALUNO, escola, turma);

        assertEquals("ana@exemplo.com", aluno.getEmail());
    }

    @Test
    void superAdminNaoTemInstituicao() {
        Usuario dono = new Usuario("Dono", "dono@exemplo.com", SENHA_HASH, Papel.SUPER_ADMIN, null, null);
        assertNull(dono.getInstituicao());

        assertThrows(IllegalArgumentException.class,
                () -> new Usuario("Dono", "dono@exemplo.com", SENHA_HASH, Papel.SUPER_ADMIN, escola, null));
    }

    @Test
    void professorAlunoEAdminPrecisamDeInstituicao() {
        for (Papel papel : new Papel[] {Papel.ADMIN_ESCOLA, Papel.PROFESSOR, Papel.ALUNO}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Usuario("Sem Escola", "x@exemplo.com", SENHA_HASH, papel, null, null));
        }
    }

    @Test
    void apenasAlunoTemTurma() {
        assertThrows(IllegalArgumentException.class,
                () -> new Usuario("Prof", "prof@exemplo.com", SENHA_HASH, Papel.PROFESSOR, escola, turma));
    }

    @Test
    void turmaPrecisaSerDaMesmaInstituicaoDoAluno() {
        Instituicao outraEscola = new Instituicao("Outra Escola");

        assertThrows(IllegalArgumentException.class,
                () -> new Usuario("Ana", "ana@exemplo.com", SENHA_HASH, Papel.ALUNO, outraEscola, turma));
    }

}
