package com.puntoventa.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas unitarias del código de verificación en dos pasos (TOTP, RFC 6238).
 */
class TotpUtilTest {

    // Secreto de los vectores de prueba del RFC 6238: el texto ASCII "12345678901234567890" en Base32.
    private static final String SECRETO_RFC = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    @DisplayName("Valida: genera el código del vector oficial del RFC 6238 (segundo 59)")
    void generaElCodigoDelVectorDelRfc() {
        // Segundo 59 -> paso 1. El RFC da 94287082 con 8 dígitos; con 6 dígitos son los últimos 6.
        assertEquals("287082", TotpUtil.generarCodigo(SECRETO_RFC, 59L / 30));
    }

    @Test
    @DisplayName("Límite: conserva los ceros a la izquierda del código")
    void conservaLosCerosAlaIzquierda() {
        // Segundo 1234567890 -> el RFC da 89005924 con 8 dígitos; con 6 dígitos es 005924.
        assertEquals("005924", TotpUtil.generarCodigo(SECRETO_RFC, 1234567890L / 30));
    }

    @Test
    @DisplayName("Valida: acepta el código del momento actual")
    void aceptaElCodigoActual() {
        long paso = Instant.now().getEpochSecond() / 30;
        String codigo = TotpUtil.generarCodigo(SECRETO_RFC, paso);

        assertTrue(TotpUtil.validarCodigo(SECRETO_RFC, codigo));
    }

    @Test
    @DisplayName("Inválido (caja negra): un código de 6 dígitos incorrecto se rechaza")
    void rechazaUnCodigoIncorrecto() {
        long paso = Instant.now().getEpochSecond() / 30;
        Set<String> aceptables = new HashSet<>();
        for (long p = paso - 2; p <= paso + 2; p++) {
            aceptables.add(TotpUtil.generarCodigo(SECRETO_RFC, p));
        }

        // Se busca un código de 6 dígitos que no coincida con ninguno de los pasos cercanos.
        int n = 0;
        String incorrecto = "000000";
        while (aceptables.contains(incorrecto)) {
            n++;
            incorrecto = String.format("%06d", n);
        }

        assertFalse(TotpUtil.validarCodigo(SECRETO_RFC, incorrecto));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" se rechaza por formato")
    @NullSource
    @ValueSource(strings = {"", "12345", "1234567", "abcdef", "123 456", " 123456"})
    @DisplayName("Inválido (particiones de equivalencia): formatos que no son 6 dígitos seguidos")
    void rechazaFormatosInvalidos(String entrada) {
        assertFalse(TotpUtil.validarCodigo(SECRETO_RFC, entrada));
    }

    @Test
    @DisplayName("Valida: el secreto nuevo tiene 32 caracteres Base32 y cambia en cada llamada")
    void generaUnSecretoBase32Valido() {
        String secreto = TotpUtil.generarSecreto();

        assertTrue(secreto.matches("[A-Z2-7]{32}"));
        assertNotEquals(secreto, TotpUtil.generarSecreto());
    }

    @Test
    @DisplayName("Valida: la URI del QR trae usuario, secreto y parámetros esperados")
    void generaLaUriDelQr() {
        String uri = TotpUtil.generarUriOtpAuth("admin", "ABC234");

        assertEquals("otpauth://totp/SistemaPuntoVenta%3Aadmin"
                + "?secret=ABC234&issuer=SistemaPuntoVenta&algorithm=SHA1&digits=6&period=30", uri);
    }
}
