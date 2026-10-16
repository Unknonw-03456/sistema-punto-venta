package com.puntoventa.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.puntoventa.database.ConexionBD;

public class TwoFactorDAO {

    public static class Estado2FA {
        public String secreto;
        public boolean activado;
    }

    public Estado2FA obtenerEstado(int idUsuario) {
        String sql = "SELECT secreto, activado FROM usuario_2fa WHERE id_usuario = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Estado2FA e = new Estado2FA();
                e.secreto = rs.getString("secreto");
                e.activado = rs.getBoolean("activado");
                return e;
            }
        } catch (Exception ex) {
            System.err.println("Error al consultar 2FA: " + ex.getMessage());
            return null;
        }
    }

    public void guardarSecretoPendiente(int idUsuario, String secreto) throws Exception {
        String sql = "INSERT INTO usuario_2fa (id_usuario, secreto, activado) VALUES (?, ?, FALSE) "
                + "ON DUPLICATE KEY UPDATE secreto = VALUES(secreto), activado = FALSE";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setString(2, secreto);
            stmt.executeUpdate();
        }
    }

    public void activar(int idUsuario) throws Exception {
        String sql = "UPDATE usuario_2fa SET activado = TRUE WHERE id_usuario = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.executeUpdate();
        }
    }

    // ===== Bloqueo temporal por intentos fallidos =====

    public LocalDateTime obtenerBloqueoHasta(int idUsuario) {
        String sql = "SELECT bloqueado_hasta FROM usuarios WHERE id_usuario = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp t = rs.getTimestamp("bloqueado_hasta");
                    return t == null ? null : t.toLocalDateTime();
                }
            }
        } catch (Exception ex) {
            System.err.println("Error al consultar bloqueo: " + ex.getMessage());
        }
        return null;
    }

    public void registrarIntentoFallido(int idUsuario) throws Exception {
        String sqlIncrementar = "UPDATE usuarios SET intentos_fallidos_2fa = intentos_fallidos_2fa + 1 WHERE id_usuario = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sqlIncrementar)) {
            stmt.setInt(1, idUsuario);
            stmt.executeUpdate();
        }

        int intentos = 0;
        String sqlConsultar = "SELECT intentos_fallidos_2fa FROM usuarios WHERE id_usuario = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sqlConsultar)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    intentos = rs.getInt("intentos_fallidos_2fa");
                }
            }
        }

        if (intentos >= 3) {
            LocalDateTime bloqueoHasta = LocalDateTime.now().plusMinutes(5);
            String sqlBloquear = "UPDATE usuarios SET bloqueado_hasta = ?, intentos_fallidos_2fa = 0 WHERE id_usuario = ?";
            try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sqlBloquear)) {
                stmt.setTimestamp(1, Timestamp.valueOf(bloqueoHasta));
                stmt.setInt(2, idUsuario);
                stmt.executeUpdate();
            }
        }
    }

    public void resetearIntentos(int idUsuario) throws Exception {
        String sql = "UPDATE usuarios SET intentos_fallidos_2fa = 0, bloqueado_hasta = NULL WHERE id_usuario = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.executeUpdate();
        }
    }
}
