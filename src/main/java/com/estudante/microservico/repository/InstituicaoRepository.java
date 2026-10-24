package com.estudante.microservico.repository;

import com.estudante.microservico.model.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstituicaoRepository extends JpaRepository<Instituicao, Long> {
}
