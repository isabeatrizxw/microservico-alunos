package com.estudante.microservico.repository;

import com.estudante.microservico.model.Convite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConviteRepository extends JpaRepository<Convite, Long> {

    Optional<Convite> findByCodigoHash(String codigoHash);

    Optional<Convite> findByIdAndInstituicaoId(Long id, Long instituicaoId);

    @Query("""
            select c from Convite c
            where c.instituicao.id = :instituicaoId
              and c.usadoEm is null and c.revogadoEm is null and c.expiraEm > :agora
            order by c.criadoEm
            """)
    List<Convite> findPendentes(@Param("instituicaoId") Long instituicaoId, @Param("agora") LocalDateTime agora);

    @Query("""
            select count(c) > 0 from Convite c
            where c.instituicao.id = :instituicaoId and c.email = :email
              and c.usadoEm is null and c.revogadoEm is null and c.expiraEm > :agora
            """)
    boolean existePendente(@Param("instituicaoId") Long instituicaoId,
                           @Param("email") String email,
                           @Param("agora") LocalDateTime agora);

    @Query("""
            select count(c) > 0 from Convite c
            where c.turma.id = :turmaId
              and c.usadoEm is null and c.revogadoEm is null and c.expiraEm > :agora
            """)
    boolean existePendenteNaTurma(@Param("turmaId") Long turmaId, @Param("agora") LocalDateTime agora);

    /** Apaga convites já usados, revogados ou vencidos de uma turma que vai ser excluída. */
    @Modifying
    @Query("delete from Convite c where c.turma.id = :turmaId")
    void deleteByTurmaId(@Param("turmaId") Long turmaId);

}
