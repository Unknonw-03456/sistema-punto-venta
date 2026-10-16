package com.puntoventa.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.puntoventa.database.ConexionBD;
import modelo.Cliente;

public class ClienteDAO {

    public ObservableList<Cliente> listar() {
        ObservableList<Cliente> lista = FXCollections.observableArrayList();
        String sql = "SELECT cedula, nombre, telefono, email FROM clientes";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Cliente(
                        rs.getString("cedula"),
                        rs.getString("nombre"),
                        rs.getString("telefono"),
                        rs.getString("email")
                ));
            }
        } catch (Exception e) {
            System.err.println("Error al cargar clientes: " + e.getMessage());
        }
        return lista;
    }

    public void crear(String cedula, String nombre, String telefono, String email) throws Exception {
        String sql = "INSERT INTO clientes (cedula, nombre, telefono, email) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, cedula);
            stmt.setString(2, nombre);
            stmt.setString(3, telefono);
            stmt.setString(4, email);
            stmt.executeUpdate();
        }
    }

    public void actualizar(String cedula, String nombre, String telefono, String email) throws Exception {
        String sql = "UPDATE clientes SET nombre=?, telefono=?, email=? WHERE cedula=?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, nombre);
            stmt.setString(2, telefono);
            stmt.setString(3, email);
            stmt.setString(4, cedula);
            stmt.executeUpdate();
        }
    }

    public void eliminar(String cedula) throws Exception {
        String sql = "DELETE FROM clientes WHERE cedula = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, cedula);
            stmt.executeUpdate();
        }
    }
}
