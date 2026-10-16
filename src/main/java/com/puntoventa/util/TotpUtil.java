package com.puntoventa.util;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Implementación mínima de TOTP (RFC 6238), compatible con Google/Microsoft Authenticator.
 * No usa librerías externas: solo javax.crypto, ya incluido en el JDK.
 */
public class TotpUtil {

    private static final int DIGITOS = 6;
    private static final int PERIODO_SEGUNDOS = 30;
    private static final String EMISOR = "SistemaPuntoVenta";

    public static String generarSecreto() {
        byte[] bytesAleatorios = new byte[20];
        new SecureRandom().nextBytes(bytesAleatorios);
        return Base32.encode(bytesAleatorios);
    }

    /** URI que se codifica en el QR; la entienden Google/Microsoft Authenticator. */
    public static String generarUriOtpAuth(String usuario, String secreto) {
        String etiqueta = URLEncoder.encode(EMISOR + ":" + usuario, StandardCharsets.UTF_8);
        String emisorCodificado = URLEncoder.encode(EMISOR, StandardCharsets.UTF_8);
        return "otpauth://totp/" + etiqueta
                + "?secret=" + secreto
                + "&issuer=" + emisorCodificado
                + "&algorithm=SHA1&digits=" + DIGITOS + "&period=" + PERIODO_SEGUNDOS;
    }

    /** URL de una imagen QR (servicio público) que representa esa URI otpauth. */
    public static String generarUrlQr(String uriOtpAuth) {
        String datos = URLEncoder.encode(uriOtpAuth, StandardCharsets.UTF_8);
        return "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + datos;
    }

    /** Valida el código ingresado permitiendo +-1 paso de 30s (el reloj del celular no es exacto). */
    public static boolean validarCodigo(String secreto, String codigoIngresado) {
        if (codigoIngresado == null || !codigoIngresado.matches("\\d{6}")) {
            return false;
        }
        long pasoActual = Instant.now().getEpochSecond() / PERIODO_SEGUNDOS;
        for (long paso = pasoActual - 1; paso <= pasoActual + 1; paso++) {
            if (codigoIngresado.equals(generarCodigo(secreto, paso))) {
                return true;
            }
        }
        return false;
    }

    private static String generarCodigo(String secreto, long paso) {
        try {
            byte[] clave = Base32.decode(secreto);
            byte[] datosContador = ByteBuffer.allocate(8).putLong(paso).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(clave, "HmacSHA1"));
            byte[] hash = mac.doFinal(datosContador);

            int desplazamiento = hash[hash.length - 1] & 0xF;
            int binario = ((hash[desplazamiento] & 0x7f) << 24)
                    | ((hash[desplazamiento + 1] & 0xff) << 16)
                    | ((hash[desplazamiento + 2] & 0xff) << 8)
                    | (hash[desplazamiento + 3] & 0xff);

            int codigo = binario % (int) Math.pow(10, DIGITOS);
            return String.format("%0" + DIGITOS + "d", codigo);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el código TOTP", e);
        }
    }
}
