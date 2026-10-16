package com.puntoventa;

import modelo.Empleado;

/**
 * Guarda el empleado que inició sesión mientras la aplicación está corriendo.
 * Es una sola variable compartida (patrón simple de "sesión en memoria"),
 * suficiente porque la app es de un solo usuario por ejecución.
 */
public class Sesion {

    private static Empleado actual;

    private Sesion() {
    }

    public static void iniciar(Empleado empleado) {
        actual = empleado;
    }

    public static Empleado getActual() {
        return actual;
    }

    public static String getRolActual() {
        return (actual != null && actual.getRol() != null) ? actual.getRol().getNombre() : null;
    }

    public static void cerrar() {
        actual = null;
    }
}
