package com.estudante.microservico.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Códigos de convite: 32 bytes aleatórios (impossíveis de adivinhar), em texto seguro para URL.
 * O banco guarda só o SHA-256 do código: quem ler o banco não consegue usar convites pendentes.
 * Não precisa de BCrypt aqui porque o código já é aleatório e longo, ao contrário de uma senha.
 */
public final class CodigoConvite {

    private static final int TAMANHO_EM_BYTES = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CodigoConvite() {
    }

    public static String gerar() {
        byte[] bytes = new byte[TAMANHO_EM_BYTES];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String codigo) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            // Toda JVM é obrigada a ter SHA-256
            throw new IllegalStateException("SHA-256 indisponível.", e);
        }
    }

}
