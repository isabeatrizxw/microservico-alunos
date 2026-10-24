package com.estudante.microservico.repository;

import com.estudante.microservico.model.ConfirmacaoEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfirmacaoEmailRepository extends JpaRepository<ConfirmacaoEmail, Long> {

    Optional<ConfirmacaoEmail> findByCodigoHash(String codigoHash);

}
