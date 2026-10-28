package com.estudante.microservico.repository;

import com.estudante.microservico.model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotaRepository extends JpaRepository<Nota, Long> {

    Optional<Nota> findByIdAndInstituicaoId(Long id, Long instituicaoId);

    boolean existsByDisciplinaId(Long disciplinaId);

    /** Todas as notas de um aluno, já com disciplina e professor carregados, agrupáveis por disciplina. */
    @Query("""
            select n from Nota n
            join fetch n.disciplina d
            join fetch n.lancadaPor
            where n.aluno.id = :alunoId
            order by d.nome, n.lancadaEm
            """)
    List<Nota> findBoletim(@Param("alunoId") Long alunoId);

    /** Notas de todos os alunos de uma turma numa disciplina, numa única consulta. */
    @Query("""
            select n from Nota n
            where n.aluno.turma.id = :turmaId and n.disciplina.id = :disciplinaId
            """)
    List<Nota> findDaTurmaNaDisciplina(@Param("turmaId") Long turmaId, @Param("disciplinaId") Long disciplinaId);

}
