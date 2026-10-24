package com.estudante.microservico.repository;

import com.estudante.microservico.model.RedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RedefinicaoSenhaRepository extends JpaRepository<RedefinicaoSenha, Long> {

    Optional<RedefinicaoSenha> findByCodigoHash(String codigoHash);

}
