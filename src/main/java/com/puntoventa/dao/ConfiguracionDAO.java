package com.puntoventa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.puntoventa.database.ConexionBD;

/** Lee los valores de la tabla "configuracion" (ITBIS, datos del negocio, prefijo de factura). */
public class ConfiguracionDAO {

    public static String obtener(Connection con, String clave, String porDefecto) {
        String sql = "SELECT valor FROM configuracion WHERE clave = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, clave);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("valor");
                }
            }
        } catch (Exception e) {
            System.err.println("Error al leer configuración: " + e.getMessage());
        }
        return porDefecto;
    }

    public static String obtener(String clave, String porDefecto) {
        return obtener(ConexionBD.getConexion(), clave, porDefecto);
    }

    public static double obtenerNumero(Connection con, String clave, double porDefecto) {
        try {
            return Double.parseDouble(obtener(con, clave, String.valueOf(porDefecto)).trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    public static double obtenerNumero(String clave, double porDefecto) {
        return obtenerNumero(ConexionBD.getConexion(), clave, porDefecto);
    }
}
