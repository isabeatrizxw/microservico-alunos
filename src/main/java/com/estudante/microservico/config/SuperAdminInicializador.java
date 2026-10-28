package com.estudante.microservico.config;

import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro SUPER_ADMIN (dono do sistema) ao iniciar, se ainda não existir nenhum.
 * E-mail e senha vêm das variáveis de ambiente SUPER_ADMIN_EMAIL e SUPER_ADMIN_SENHA — nunca do código.
 */
@Component
public class SuperAdminInicializador implements ApplicationRunner {

    static final int TAMANHO_MINIMO_SENHA = 8;

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInicializador.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public SuperAdminInicializador(UsuarioRepository usuarioRepository,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${app.super-admin.email:}") String email,
                                   @Value("${app.super-admin.senha:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByPapel(Papel.SUPER_ADMIN)) {
            return;
        }
        if (email.isBlank() || senha.isBlank()) {
            log.warn("Nenhum SUPER_ADMIN cadastrado. Defina SUPER_ADMIN_EMAIL e SUPER_ADMIN_SENHA para criar o primeiro.");
            return;
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalStateException("SUPER_ADMIN_SENHA precisa ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        Usuario superAdmin = new Usuario("Administrador do sistema", email, passwordEncoder.encode(senha),
                Papel.SUPER_ADMIN, null, null);
        usuarioRepository.save(superAdmin);
        log.info("SUPER_ADMIN criado: {}", superAdmin.getEmail());
    }

}
