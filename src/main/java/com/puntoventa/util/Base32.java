package com.puntoventa.util;

import java.io.ByteArrayOutputStream;

/**
 * Codificador/decodificador Base32 (RFC 4648), sin dependencias externas.
 * Se usa para el secreto TOTP, que las apps Authenticator esperan en Base32.
 */
public class Base32 {

    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public static String encode(byte[] datos) {
        StringBuilder resultado = new StringBuilder();
        int buffer = 0;
        int bitsEnBuffer = 0;

        for (byte b : datos) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsEnBuffer += 8;
            while (bitsEnBuffer >= 5) {
                int indice = (buffer >> (bitsEnBuffer - 5)) & 0x1F;
                resultado.append(ALFABETO.charAt(indice));
                bitsEnBuffer -= 5;
            }
        }
        if (bitsEnBuffer > 0) {
            int indice = (buffer << (5 - bitsEnBuffer)) & 0x1F;
            resultado.append(ALFABETO.charAt(indice));
        }
        return resultado.toString();
    }

    public static byte[] decode(String base32) {
        String limpio = base32.trim().toUpperCase().replace("=", "");
        int buffer = 0;
        int bitsEnBuffer = 0;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        for (char c : limpio.toCharArray()) {
            int indice = ALFABETO.indexOf(c);
            if (indice < 0) {
                continue;
            }
            buffer = (buffer << 5) | indice;
            bitsEnBuffer += 5;
            if (bitsEnBuffer >= 8) {
                salida.write((buffer >> (bitsEnBuffer - 8)) & 0xFF);
                bitsEnBuffer -= 8;
            }
        }
        return salida.toByteArray();
    }
}
