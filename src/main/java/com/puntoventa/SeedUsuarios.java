package com.puntoventa;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.puntoventa.dao.UsuarioDAO;
import com.puntoventa.database.ConexionBD;

/**
 * Clase de un solo uso: crea los 3 usuarios de prueba (uno por rol) con la
 * contraseña ya hasheada con BCrypt. Córrela UNA VEZ desde tu IDE
 * (clic derecho > Run) después de ejecutar el ALTER TABLE de "activo".
 *
 * No se agrega a module-info ni se usa desde la app en ejecución normal.
 */
public class SeedUsuarios {

    private static final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public static void main(String[] args) throws Exception {
        crearSiNoExiste("admin", "admin123", "Administrador General", "Administrador", "Administrador");
        crearSiNoExiste("supervisor", "super123", "Supervisor de Tienda", "Supervisor", "Supervisor");
        crearSiNoExiste("operador", "oper123", "Operador de Caja", "Operador", "Operador");

        System.out.println("Listo. Usuarios de prueba creados (o ya existían).");
        System.out.println("admin / admin123");
        System.out.println("supervisor / super123");
        System.out.println("operador / oper123");
    }

    private static void crearSiNoExiste(String usuario, String contrasenaPlano, String nombre, String cargo, String nombreRol) throws Exception {
        if (existeUsuario(usuario)) {
            System.out.println("Ya existe: " + usuario + " (se omite)");
            return;
        }

        int idRol = obtenerIdRol(nombreRol);
        usuarioDAO.crearUsuario(usuario, contrasenaPlano, nombre, cargo, idRol);
        System.out.println("Creado: " + usuario);
    }

    private static boolean existeUsuario(String usuario) throws Exception {
        String sql = "SELECT 1 FROM usuarios WHERE usuario = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int obtenerIdRol(String nombreRol) throws Exception {
        String sql = "SELECT id_rol FROM roles WHERE nombre = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, nombreRol);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_rol");
                }
                throw new RuntimeException("No existe el rol '" + nombreRol + "'.");
            }
        }
    }
}
