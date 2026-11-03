package com.puntoventa.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.puntoventa.database.ConexionBD;
import modelo.Categoria;
import modelo.Producto;
import modelo.Proveedor;

public class ProductoDAO {

    /** Lista los productos con su categoría y proveedor (para poder preseleccionarlos al editar). */
    public ObservableList<Producto> listar() {
        ObservableList<Producto> lista = FXCollections.observableArrayList();
        String sql = "SELECT p.codigo, p.nombre, p.precio, p.stock, "
                + "p.id_categoria, c.nombre AS categoria_nombre, "
                + "p.id_proveedor, pr.nombre AS proveedor_nombre "
                + "FROM productos p "
                + "LEFT JOIN categorias c ON c.id_categoria = p.id_categoria "
                + "LEFT JOIN proveedores pr ON pr.id_proveedor = p.id_proveedor "
                + "ORDER BY p.codigo";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Categoria categoria = new Categoria(rs.getInt("id_categoria"), rs.getString("categoria_nombre"), null);
                Proveedor proveedor = new Proveedor(rs.getInt("id_proveedor"), rs.getString("proveedor_nombre"), null);
                lista.add(new Producto(
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getDouble("precio"),
                        rs.getInt("stock"),
                        categoria,
                        proveedor
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
