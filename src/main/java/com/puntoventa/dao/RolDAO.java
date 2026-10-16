package com.puntoventa.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.puntoventa.database.ConexionBD;
import modelo.Rol;

/**
 * Acceso a datos de la tabla roles.
 */
public class RolDAO {

    public List<Rol> listarRoles() {
        List<Rol> roles = new ArrayList<>();
        String sql = "SELECT id_rol, nombre FROM roles ORDER BY id_rol";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                roles.add(new Rol(rs.getInt("id_rol"), rs.getString("nombre")));
            }
        } catch (Exception e) {
            System.err.println("Error al cargar roles: " + e.getMessage());
        }
        return roles;
    }
}
