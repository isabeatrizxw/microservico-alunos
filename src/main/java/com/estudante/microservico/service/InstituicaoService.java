package com.estudante.microservico.service;

import com.estudante.microservico.dto.InstituicaoResponseDTO;
import com.estudante.microservico.dto.NovaInstituicaoRequestDTO;
import com.estudante.microservico.exception.ConflitoException;
import com.estudante.microservico.model.Instituicao;
import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.InstituicaoRepository;
import com.estudante.microservico.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Cadastro de escolas, feito pelo SUPER_ADMIN (dono do sistema) ao vender para uma escola nova. */
@Service
public class InstituicaoService {

    private final InstituicaoRepository instituicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public InstituicaoService(InstituicaoRepository instituicaoRepository,
                              UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder) {
        this.instituicaoRepository = instituicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria a instituição e a conta do seu administrador (ADMIN_ESCOLA) numa única transação:
     * se o administrador não puder ser criado, a instituição também não fica salva.
     */
    @Transactional
    public InstituicaoResponseDTO criar(NovaInstituicaoRequestDTO pedido) {
        String emailAdmin = Usuario.normalizarEmail(pedido.adminEmail());
        if (usuarioRepository.existsByEmail(emailAdmin)) {
            throw new ConflitoException("Já existe um usuário com este e-mail.");
        }

        Instituicao instituicao = instituicaoRepository.save(new Instituicao(pedido.nome().trim()));
        usuarioRepository.save(new Usuario(pedido.adminNome().trim(), emailAdmin,
                passwordEncoder.encode(pedido.adminSenha()), Papel.ADMIN_ESCOLA, instituicao, null));
        return InstituicaoResponseDTO.de(instituicao);
    }

    @Transactional(readOnly = true)
    public List<InstituicaoResponseDTO> listar() {
        return instituicaoRepository.findAll(Sort.by("nome")).stream()
                .map(InstituicaoResponseDTO::de)
                .toList();
    }

}
