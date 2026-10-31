package com.puntoventa.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de Base32, el formato en que se guarda el secreto del 2FA.
 */
class Base32Test {

    @Test
    @DisplayName("Valida: codifica 'foobar' igual que el vector de prueba del RFC 4648")
    void codificaElVectorDelRfc() {
        String resultado = Base32.encode("foobar".getBytes(StandardCharsets.UTF_8));

        // El RFC escribe MZXW6YTBOI====== ; esta implementación no agrega el relleno '='.
        assertEquals("MZXW6YTBOI", resultado);
    }

    @Test
    @DisplayName("Valida: decodificar lo que se codificó devuelve los mismos bytes")
    void codificarYDecodificarEsReversible() {
        byte[] original = {0, 1, 2, (byte) 250, (byte) 255, 42, 7};

        byte[] recuperado = Base32.decode(Base32.encode(original));

        assertArrayEquals(original, recuperado);
    }

    @Test
    @DisplayName("Valida: decodifica en minúsculas y con relleno '='")
    void decodificaMinusculasYRelleno() {
        byte[] bytes = Base32.decode("mzxw6ytboi======");

        assertEquals("foobar", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Límite: un texto vacío da un arreglo vacío, y viceversa")
    void textoVacioDaArregloVacio() {
        assertEquals(0, Base32.decode("").length);
        assertEquals("", Base32.encode(new byte[0]));
    }
}
