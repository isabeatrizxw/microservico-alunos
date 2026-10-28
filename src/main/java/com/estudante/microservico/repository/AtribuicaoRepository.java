package com.estudante.microservico.repository;

import com.estudante.microservico.model.Atribuicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AtribuicaoRepository extends JpaRepository<Atribuicao, Long> {

    boolean existsByProfessorIdAndTurmaIdAndDisciplinaId(Long professorId, Long turmaId, Long disciplinaId);

    boolean existsByProfessorIdAndTurmaId(Long professorId, Long turmaId);

    boolean existsByTurmaId(Long turmaId);

    boolean existsByDisciplinaId(Long disciplinaId);

    Optional<Atribuicao> findByIdAndInstituicaoId(Long id, Long instituicaoId);

    @Query("""
            select a from Atribuicao a
            join fetch a.turma t
            join fetch a.disciplina d
            where a.professor.id = :professorId
            order by t.nome, d.nome
            """)
    List<Atribuicao> findDoProfessor(@Param("professorId") Long professorId);

    @Query("""
            select a from Atribuicao a
            join fetch a.professor
            join fetch a.turma t
            join fetch a.disciplina d
            where a.instituicao.id = :instituicaoId
            order by t.nome, d.nome
            """)
    List<Atribuicao> findDaInstituicao(@Param("instituicaoId") Long instituicaoId);

}
