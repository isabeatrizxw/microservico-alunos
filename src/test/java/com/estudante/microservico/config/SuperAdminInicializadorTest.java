package com.estudante.microservico.config;

import com.estudante.microservico.model.Papel;
import com.estudante.microservico.model.Usuario;
import com.estudante.microservico.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuperAdminInicializadorTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void criaSuperAdminComSenhaCriptografadaQuandoNaoExisteNenhum() {
        when(usuarioRepository.existsByPapel(Papel.SUPER_ADMIN)).thenReturn(false);

        inicializador("Dono@Exemplo.com", "senha-forte-123").run(null);

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertEquals(Papel.SUPER_ADMIN, salvo.getValue().getPapel());
        assertEquals("dono@exemplo.com", salvo.getValue().getEmail());
        assertNotEquals("senha-forte-123", salvo.getValue().getSenhaHash());
        assertTrue(passwordEncoder.matches("senha-forte-123", salvo.getValue().getSenhaHash()));
    }

    @Test
    void naoFazNadaSeJaExisteSuperAdmin() {
        when(usuarioRepository.existsByPapel(Papel.SUPER_ADMIN)).thenReturn(true);

        inicializador("dono@exemplo.com", "senha-forte-123").run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoCriaSemAsVariaveisDeAmbiente() {
        when(usuarioRepository.existsByPapel(Papel.SUPER_ADMIN)).thenReturn(false);

        inicializador("", "").run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void recusaSenhaCurta() {
        when(usuarioRepository.existsByPapel(Papel.SUPER_ADMIN)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> inicializador("dono@exemplo.com", "1234567").run(null));
        verify(usuarioRepository, never()).save(any());
    }

    private SuperAdminInicializador inicializador(String email, String senha) {
        return new SuperAdminInicializador(usuarioRepository, passwordEncoder, email, senha);
    }

}
