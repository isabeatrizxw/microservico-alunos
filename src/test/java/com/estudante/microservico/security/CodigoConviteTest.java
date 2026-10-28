package com.estudante.microservico.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodigoConviteTest {

    @Test
    void geraCodigoLongoSeguroParaUrlEDiferenteACadaVez() {
        String primeiro = CodigoConvite.gerar();
        String segundo = CodigoConvite.gerar();

        assertEquals(43, primeiro.length());
        assertTrue(primeiro.matches("[A-Za-z0-9_-]+"));
        assertNotEquals(primeiro, segundo);
    }

    @Test
    void hashEhSha256EmHexadecimalESempreIgualParaOMesmoCodigo() {
        String codigo = CodigoConvite.gerar();

        String hash = CodigoConvite.hash(codigo);

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
        assertEquals(hash, CodigoConvite.hash(codigo));
        assertNotEquals(codigo, hash);
    }

}
