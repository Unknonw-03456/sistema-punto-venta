package com.puntoventa.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.puntoventa.database.ConexionBD;
import modelo.Producto;

public class ProductoDAO {

    public ObservableList<Producto> listar() {
        ObservableList<Producto> lista = FXCollections.observableArrayList();
        String sql = "SELECT codigo, nombre, precio, stock FROM productos";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Producto(
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getDouble("precio"),
                        rs.getInt("stock"),
                        null,
                        null
                ));
            }
        } catch (Exception e) {
            System.err.println("Error al cargar productos: " + e.getMessage());
        }
        return lista;
    }

    public void crear(String codigo, String nombre, double precio, int stock, int idCategoria, int idProveedor) throws Exception {
        String sql = "INSERT INTO productos (codigo, nombre, precio, stock, id_categoria, id_proveedor) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, codigo);
            stmt.setString(2, nombre);
            stmt.setDouble(3, precio);
            stmt.setInt(4, stock);
            stmt.setInt(5, idCategoria);
            stmt.setInt(6, idProveedor);
            stmt.executeUpdate();
        }
    }

    public void actualizar(String codigo, String nombre, double precio, int stock, int idCategoria, int idProveedor) throws Exception {
        String sql = "UPDATE productos SET nombre=?, precio=?, stock=?, id_categoria=?, id_proveedor=? WHERE codigo=?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, nombre);
            stmt.setDouble(2, precio);
            stmt.setInt(3, stock);
            stmt.setInt(4, idCategoria);
            stmt.setInt(5, idProveedor);
            stmt.setString(6, codigo);
            stmt.executeUpdate();
        }
    }

    public void eliminar(String codigo) throws Exception {
        String sql = "DELETE FROM productos WHERE codigo = ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, codigo);
            stmt.executeUpdate();
        }
    }
}
