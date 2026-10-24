package com.estudante.microservico.repository;

import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Os e-mails ficam salvos normalizados (ver {@link Usuario#normalizarEmail(String)});
 * normalize o e-mail recebido antes de consultar.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPapel(Papel papel);

    Optional<Usuario> findByIdAndInstituicaoIdAndPapel(Long id, Long instituicaoId, Papel papel);

    List<Usuario> findByTurmaIdOrderByNome(Long turmaId);

    /** Alunos ativos da turma (desativados não aparecem nas telas do professor). */
    List<Usuario> findByTurmaIdAndAtivoTrueOrderByNome(Long turmaId);

    boolean existsByTurmaId(Long turmaId);

    Optional<Usuario> findByIdAndInstituicaoId(Long id, Long instituicaoId);

    /** Pessoas de um papel na escola, já com a turma carregada (uma consulta só). */
    @Query("""
            select u from Usuario u
            left join fetch u.turma
            where u.instituicao.id = :instituicaoId and u.papel = :papel
            order by u.nome
            """)
    List<Usuario> listarPorPapel(@Param("instituicaoId") Long instituicaoId, @Param("papel") Papel papel);

}
