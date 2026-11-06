package com.puntoventa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * La sesión guarda el id del USUARIO, pero las tablas de caja y ventas se enlazan con el
 * id del EMPLEADO. Este método hace esa traducción.
 */
public class EmpleadoDAO {

    public static int obtenerIdEmpleado(Connection con, int idUsuario) throws Exception {
        String sql = "SELECT id_empleado FROM empleados WHERE id_usuario = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_empleado");
                }
            }
        }
        throw new Exception("El usuario no tiene un empleado asociado.");
    }
}
