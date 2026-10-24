package com.estudante.microservico.repository;

import com.estudante.microservico.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

    List<Turma> findByInstituicaoIdOrderByNome(Long instituicaoId);

    boolean existsByInstituicaoIdAndNomeIgnoreCase(Long instituicaoId, String nome);

    Optional<Turma> findByIdAndInstituicaoId(Long id, Long instituicaoId);

}
