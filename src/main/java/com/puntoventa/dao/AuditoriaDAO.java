package com.puntoventa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

/** Registra en la tabla "auditoria" las operaciones críticas: quién, qué y cuándo. */
public class AuditoriaDAO {

    /**
     * Se usa la MISMA conexión de la operación para que el registro de auditoría
     * quede dentro de la misma transacción (si la operación se revierte, no queda registro).
     */
    public static void registrar(Connection con, int idUsuario, String accion, String entidad,
                                 String referencia, String detalle) throws Exception {
        String sql = "INSERT INTO auditoria (fecha_hora, id_usuario, accion, entidad, referencia, detalle) "
                + "VALUES (NOW(), ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setString(2, accion);
            stmt.setString(3, entidad);
            stmt.setString(4, referencia);
            stmt.setString(5, detalle != null && detalle.length() > 255 ? detalle.substring(0, 255) : detalle);
            stmt.executeUpdate();
        }
    }
}
