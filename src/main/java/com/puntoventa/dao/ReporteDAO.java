package com.puntoventa.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.puntoventa.database.ConexionBD;

/**
 * Consultas usadas solo para armar el informe PDF (filtradas por rango de fecha_registro).
 * No reemplaza a las tablas de la UI: son lecturas independientes para el reporte.
 */
public class ReporteDAO {

    public static class FilaProducto {
        public String codigo;
        public String nombre;
        public double precio;
        public int stock;
    }

    public static class FilaCliente {
        public String cedula;
        public String nombre;
        public String telefono;
        public String email;
    }

    public List<FilaProducto> obtenerProductos(LocalDate desde, LocalDate hasta) {
        List<FilaProducto> lista = new ArrayList<>();
        String sql = "SELECT codigo, nombre, precio, stock FROM productos "
                + "WHERE fecha_registro BETWEEN ? AND ? ORDER BY nombre";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FilaProducto f = new FilaProducto();
                    f.codigo = rs.getString("codigo");
                    f.nombre = rs.getString("nombre");
                    f.precio = rs.getDouble("precio");
                    f.stock = rs.getInt("stock");
                    lista.add(f);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al obtener productos para el informe: " + e.getMessage());
        }
        return lista;
    }

    public List<FilaCliente> obtenerClientes(LocalDate desde, LocalDate hasta) {
        List<FilaCliente> lista = new ArrayList<>();
        String sql = "SELECT cedula, nombre, telefono, email FROM clientes "
                + "WHERE fecha_registro BETWEEN ? AND ? ORDER BY nombre";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FilaCliente f = new FilaCliente();
                    f.cedula = rs.getString("cedula");
                    f.nombre = rs.getString("nombre");
                    f.telefono = rs.getString("telefono");
                    f.email = rs.getString("email");
                    lista.add(f);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al obtener clientes para el informe: " + e.getMessage());
        }
        return lista;
    }
}
