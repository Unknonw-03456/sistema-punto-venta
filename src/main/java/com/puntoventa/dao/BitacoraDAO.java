package com.puntoventa.dao;

import java.sql.PreparedStatement;
import java.sql.Types;

import com.puntoventa.database.ConexionBD;

public class BitacoraDAO {

    public void registrar(String usuarioIntento, Integer idUsuario, boolean exito, String detalle) {
        String sql = "INSERT INTO bitacora_login (usuario_intento, id_usuario, exito, detalle) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, usuarioIntento);
            if (idUsuario == null) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, idUsuario);
            }
            stmt.setBoolean(3, exito);
            stmt.setString(4, detalle);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error al registrar bitácora: " + e.getMessage());
        }
    }
}
