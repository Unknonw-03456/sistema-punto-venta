package com.puntoventa.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.puntoventa.database.ConexionBD;
import modelo.Categoria;

public class CategoriaDAO {

    public ObservableList<Categoria> listar() {
        ObservableList<Categoria> lista = FXCollections.observableArrayList();
        String sql = "SELECT id_categoria, nombre, descripcion FROM categorias";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Categoria(
                        rs.getInt("id_categoria"),
                        rs.getString("nombre"),
                        rs.getString("descripcion")
                ));
            }
        } catch (Exception e) {
            System.err.println("Error al cargar categorías: " + e.getMessage());
        }
        return lista;
    }

    public void crear(String nombre, String descripcion) throws Exception {
        String sql = "INSERT INTO categorias (nombre, descripcion) VALUES (?, ?)";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setString(1, nombre);
            stmt.setString(2, descripcion);
            stmt.executeUpdate();
        }
    }
}
