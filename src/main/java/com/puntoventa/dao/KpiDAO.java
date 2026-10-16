package com.puntoventa.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.puntoventa.database.ConexionBD;

/**
 * Consultas agregadas (conteos, sumas, agrupaciones) para los KPIs y gráficos del dashboard.
 */
public class KpiDAO {

    private static final int UMBRAL_STOCK_BAJO = 10;

    public int contarProductos() {
        return contarFilas("SELECT COUNT(*) AS total FROM productos");
    }

    public int contarClientes() {
        return contarFilas("SELECT COUNT(*) AS total FROM clientes");
    }

    public int contarCategorias() {
        return contarFilas("SELECT COUNT(*) AS total FROM categorias");
    }

    public int contarStockBajo() {
        String sql = "SELECT COUNT(*) AS total FROM productos WHERE stock < ?";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setInt(1, UMBRAL_STOCK_BAJO);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        } catch (Exception e) {
            System.err.println("Error al contar stock bajo: " + e.getMessage());
            return 0;
        }
    }

    public double calcularValorInventario() {
        String sql = "SELECT SUM(precio * stock) AS total FROM productos";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getDouble("total") : 0;
        } catch (Exception e) {
            System.err.println("Error al calcular valor de inventario: " + e.getMessage());
            return 0;
        }
    }

    public Map<String, Integer> productosPorCategoria() {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        String sql = "SELECT c.nombre AS categoria, COUNT(p.codigo) AS cantidad "
                + "FROM categorias c LEFT JOIN productos p ON p.id_categoria = c.id_categoria "
                + "GROUP BY c.id_categoria, c.nombre ORDER BY c.nombre";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                resultado.put(rs.getString("categoria"), rs.getInt("cantidad"));
            }
        } catch (Exception e) {
            System.err.println("Error al agrupar productos por categoría: " + e.getMessage());
        }
        return resultado;
    }

    public Map<String, Integer> stockPorCategoria() {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        String sql = "SELECT c.nombre AS categoria, COALESCE(SUM(p.stock), 0) AS stock_total "
                + "FROM categorias c LEFT JOIN productos p ON p.id_categoria = c.id_categoria "
                + "GROUP BY c.id_categoria, c.nombre ORDER BY c.nombre";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                resultado.put(rs.getString("categoria"), rs.getInt("stock_total"));
            }
        } catch (Exception e) {
            System.err.println("Error al agrupar stock por categoría: " + e.getMessage());
        }
        return resultado;
    }

    private int contarFilas(String sql) {
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        } catch (Exception e) {
            System.err.println("Error al contar filas: " + e.getMessage());
            return 0;
        }
    }
}
