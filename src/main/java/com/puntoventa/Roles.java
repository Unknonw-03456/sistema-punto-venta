package com.puntoventa;

/**
 * Nombres de los roles tal como están guardados en la tabla "roles".
 * Se usan estas constantes en lugar de escribir el texto en cada archivo,
 * así un cambio de nombre se hace en un solo lugar.
 */
public final class Roles {

    public static final String ADMINISTRADOR = "Administrador";
    public static final String SUPERVISOR = "Supervisor";
    public static final String CAJERO = "Cajero";
    public static final String ENCARGADO_INVENTARIO = "Encargado de inventario";

    private Roles() {
    }
}
