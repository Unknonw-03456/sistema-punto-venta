package com.puntoventa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import com.puntoventa.database.ConexionBD;
import modelo.Empleado;
import modelo.Rol;
import modelo.UsuarioAdmin;

/**
 * Acceso a datos de usuarios/empleados.
 * El login vive en "usuarios" (usuario + contraseña hasheada); el nombre, cargo
 * y rol viven en "empleados", enlazada por id_usuario. Las contraseñas se
 * guardan y verifican siempre con hash BCrypt, nunca en texto plano.
 */
public class UsuarioDAO {

    /**
     * Valida usuario/contraseña contra el hash guardado.
     * Devuelve el Empleado (con su rol) si las credenciales son correctas y la cuenta está activa,
     * o null si el usuario no existe, está desactivado, no tiene un empleado asociado,
     * o la contraseña no coincide.
     */
    public Empleado autenticar(String usuario, String contrasenaPlano) {
        String sql = "SELECT u.id_usuario, u.usuario, u.contrasena, u.activo, "
                + "e.nombre, e.cargo, r.id_rol, r.nombre AS rol_nombre "
                + "FROM usuarios u "
                + "JOIN empleados e ON e.id_usuario = u.id_usuario "
                + "JOIN roles r ON r.id_rol = e.id_rol "
                + "WHERE u.usuario = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null; // no existe ese usuario, o no tiene empleado asociado
                }
                if (!rs.getBoolean("activo")) {
                    return null; // cuenta desactivada
                }
                String hashGuardado = rs.getString("contrasena");
                if (!BCrypt.checkpw(contrasenaPlano, hashGuardado)) {
                    return null; // contraseña incorrecta
                }
                Rol rol = new Rol(rs.getInt("id_rol"), rs.getString("rol_nombre"));
                return new Empleado(
                        rs.getInt("id_usuario"),
                        rs.getString("usuario"),
                        null, // nunca se guarda la contraseña en texto plano en memoria
                        rs.getString("nombre"),
                        rs.getString("cargo"),
                        rol
                );
            }
        } catch (Exception e) {
            System.err.println("Error al autenticar: " + e.getMessage());
            return null;
        }
    }

    /** Lista para la tabla del módulo de administración de usuarios (solo Administrador). */
    public List<UsuarioAdmin> listarUsuariosAdmin() {
        List<UsuarioAdmin> lista = new ArrayList<>();
        String sql = "SELECT u.id_usuario, u.usuario, u.activo, e.nombre, e.cargo, r.nombre AS rol_nombre "
                + "FROM usuarios u "
                + "JOIN empleados e ON e.id_usuario = u.id_usuario "
                + "JOIN roles r ON r.id_rol = e.id_rol "
                + "ORDER BY u.usuario";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new UsuarioAdmin(
                        rs.getInt("id_usuario"),
                        rs.getString("usuario"),
                        rs.getString("nombre"),
                        rs.getString("cargo"),
                        rs.getString("rol_nombre"),
                        rs.getBoolean("activo")
                ));
            }
        } catch (Exception e) {
            System.err.println("Error al listar usuarios: " + e.getMessage());
        }
        return lista;
    }

    /** Crea el login (usuarios) y el perfil (empleados) en un solo paso. */
    public void crearUsuario(String usuario, String contrasenaPlano, String nombre, String cargo, int idRol) throws Exception {
        String hash = BCrypt.hashpw(contrasenaPlano, BCrypt.gensalt());
        Connection con = ConexionBD.getConexion();

        int idUsuarioGenerado;
        String sqlUsuario = "INSERT INTO usuarios (usuario, contrasena, activo) VALUES (?, ?, TRUE)";
        try (PreparedStatement stmt = con.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, usuario);
            stmt.setString(2, hash);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new Exception("No se pudo obtener el id del usuario creado.");
                }
                idUsuarioGenerado = keys.getInt(1);
            }
        }

        String sqlEmpleado = "INSERT INTO empleados (id_usuario, nombre, cargo, id_rol) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = con.prepareStatement(sqlEmpleado)) {
            stmt.setInt(1, idUsuarioGenerado);
            stmt.setString(2, nombre);
            stmt.setString(3, cargo);
            stmt.setInt(4, idRol);
            stmt.executeUpdate();
        }
    }

    public void cambiarEstado(int idUsuario, boolean activo) throws Exception {
        String sql = "UPDATE usuarios SET activo = ? WHERE id_usuario = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setBoolean(1, activo);
            stmt.setInt(2, idUsuario);
            stmt.executeUpdate();
        }
    }
}
