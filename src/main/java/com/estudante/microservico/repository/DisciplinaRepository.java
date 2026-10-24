package com.estudante.microservico.repository;

import com.estudante.microservico.model.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {

    List<Disciplina> findByInstituicaoIdOrderByNome(Long instituicaoId);

    boolean existsByInstituicaoIdAndNomeIgnoreCase(Long instituicaoId, String nome);

    Optional<Disciplina> findByIdAndInstituicaoId(Long id, Long instituicaoId);

}
