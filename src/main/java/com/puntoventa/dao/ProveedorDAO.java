package com.puntoventa.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.puntoventa.database.ConexionBD;
import modelo.Proveedor;

/** Acceso a datos de proveedores. */
public class ProveedorDAO {

    public ObservableList<Proveedor> listar() {
        ObservableList<Proveedor> lista = FXCollections.observableArrayList();
        String sql = "SELECT id_proveedor, nombre, telefono FROM proveedores ORDER BY nombre";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Proveedor(
                        rs.getInt("id_proveedor"),
                        rs.getString("nombre"),
                        rs.getString("telefono")
                ));
            }
        } catch (Exception e) {
            System.err.println("Error al cargar proveedores: " + e.getMessage());
        }
        return lista;
    }

    public void crear(String nombre, String telefono) throws Exception {
        String sql = "INSERT INTO proveedores (nombre, telefono) VALUES (?, ?)";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, nombre);
            stmt.setString(2, telefono);
            stmt.executeUpdate();
        }
    }
}
