package com.puntoventa.service;

import com.puntoventa.Roles;

/**
 * Política de descuentos por rol (mejora de la práctica de regresión).
 * El Cajero puede aplicar como máximo el 10 %; Administrador y Supervisor, hasta 100 %.
 * Lógica pura: se prueba con JUnit y se valida también en el backend (VentaDAO).
 */
public final class PoliticaDescuento {

    public static final double MAXIMO_CAJERO = 10.0;

    private PoliticaDescuento() {
    }

    public static double maximoPara(String rol) {
        if (Roles.ADMINISTRADOR.equals(rol) || Roles.SUPERVISOR.equals(rol)) {
            return 100.0;
        }
        if (Roles.CAJERO.equals(rol)) {
            return MAXIMO_CAJERO;
        }
        return 0.0; // cualquier otro rol (o rol desconocido) no puede dar descuentos
    }

    /** @throws IllegalArgumentException si el descuento supera lo permitido al rol */
    public static void validar(String rol, double descuentoPorcentaje) {
        double maximo = maximoPara(rol);
        if (descuentoPorcentaje > maximo) {
            throw new IllegalArgumentException("Tu rol solo permite un descuento máximo de "
                    + (maximo == Math.floor(maximo) ? String.valueOf((int) maximo) : String.valueOf(maximo))
                    + "%. Pide autorización a un Supervisor.");
        }
    }
}
