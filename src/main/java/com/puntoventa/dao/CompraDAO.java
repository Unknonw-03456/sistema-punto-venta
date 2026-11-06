package com.puntoventa.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.puntoventa.database.ConexionBD;
import modelo.CompraResumen;
import modelo.LineaCompra;

/**
 * Compras a proveedores. Al registrar una compra, en UNA transacción: se guarda la compra y su
 * detalle, SUBE el stock de cada producto, se actualiza su costo (último costo de compra),
 * se registra el movimiento de inventario y la auditoría.
 */
public class CompraDAO {

    public int registrar(int idUsuario, int idProveedor, String numeroDocumento, List<LineaCompra> lineasEntrada) throws Exception {
        if (lineasEntrada == null || lineasEntrada.isEmpty()) {
            throw new Exception("La compra no tiene productos.");
        }
        List<LineaCompra> lineas = new ArrayList<>(lineasEntrada);
        lineas.sort(Comparator.comparing(LineaCompra::getCodigo)); // orden fijo para evitar bloqueos cruzados

        BigDecimal total = BigDecimal.ZERO;
        for (LineaCompra l : lineas) {
            if (l.getCantidad() <= 0) {
                throw new Exception("La cantidad de \"" + l.getNombre() + "\" debe ser mayor a 0.");
            }
            if (Double.isNaN(l.getCostoUnitario()) || l.getCostoUnitario() <= 0) {
                throw new Exception("El costo de \"" + l.getNombre() + "\" debe ser mayor a 0.");
            }
            total = total.add(BigDecimal.valueOf(l.getCostoUnitario())
                    .multiply(BigDecimal.valueOf(l.getCantidad())).setScale(2, RoundingMode.HALF_UP));
        }

        Connection con = ConexionBD.getConexion();
        boolean autoPrevio = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int idEmpleado = EmpleadoDAO.obtenerIdEmpleado(con, idUsuario);

            try (PreparedStatement stmt = con.prepareStatement("SELECT 1 FROM proveedores WHERE id_proveedor = ?")) {
                stmt.setInt(1, idProveedor);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new Exception("El proveedor seleccionado ya no existe.");
                    }
                }
            }

            for (LineaCompra l : lineas) {
                try (PreparedStatement stmt = con.prepareStatement(
                        "SELECT activo FROM productos WHERE codigo = ? FOR UPDATE")) {
                    stmt.setString(1, l.getCodigo());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new Exception("El producto " + l.getCodigo() + " ya no existe.");
                        }
                        if (!rs.getBoolean("activo")) {
                            throw new Exception("El producto \"" + l.getNombre() + "\" está desactivado.");
                        }
                    }
                }
            }

            int idCompra;
            try (PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO compras (fecha, total, id_proveedor, id_empleado, numero_documento) VALUES (NOW(), ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                stmt.setBigDecimal(1, total);
                stmt.setInt(2, idProveedor);
                stmt.setInt(3, idEmpleado);
                String doc = numeroDocumento == null || numeroDocumento.isBlank() ? null : numeroDocumento.trim();
                stmt.setString(4, doc != null && doc.length() > 30 ? doc.substring(0, 30) : doc);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    idCompra = keys.getInt(1);
                }
            }

            String referencia = "COMP-" + idCompra;
            for (LineaCompra l : lineas) {
                try (PreparedStatement stmt = con.prepareStatement(
                        "INSERT INTO detalle_compra (id_compra, codigo_producto, cantidad, costo_unitario) VALUES (?, ?, ?, ?)")) {
                    stmt.setInt(1, idCompra);
                    stmt.setString(2, l.getCodigo());
                    stmt.setInt(3, l.getCantidad());
                    stmt.setDouble(4, l.getCostoUnitario());
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = con.prepareStatement(
                        "UPDATE productos SET stock = stock + ?, costo = ? WHERE codigo = ?")) {
                    stmt.setInt(1, l.getCantidad());
                    stmt.setDouble(2, l.getCostoUnitario());
                    stmt.setString(3, l.getCodigo());
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = con.prepareStatement(
                        "INSERT INTO movimientos_inventario (codigo_producto, fecha, tipo, cantidad, referencia, id_empleado) "
                                + "VALUES (?, NOW(), 'COMPRA', ?, ?, ?)")) {
                    stmt.setString(1, l.getCodigo());
                    stmt.setInt(2, l.getCantidad());
                    stmt.setString(3, referencia);
                    stmt.setInt(4, idEmpleado);
                    stmt.executeUpdate();
                }
            }

            AuditoriaDAO.registrar(con, idUsuario, "COMPRA", "compra", referencia,
                    "Total " + total + " | " + lineas.size() + " producto(s)");
            con.commit();
            return idCompra;
        } catch (Exception e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(autoPrevio);
        }
    }

    public List<CompraResumen> historial() {
        List<CompraResumen> lista = new ArrayList<>();
        String sql = "SELECT c.id_compra, c.fecha, pr.nombre AS proveedor, c.numero_documento, c.total "
                + "FROM compras c JOIN proveedores pr ON pr.id_proveedor = c.id_proveedor "
                + "ORDER BY c.id_compra DESC LIMIT 50";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new CompraResumen(rs.getInt("id_compra"), rs.getTimestamp("fecha").toLocalDateTime(),
                        rs.getString("proveedor"), rs.getString("numero_documento"), rs.getDouble("total")));
            }
        } catch (Exception e) {
            System.err.println("Error al cargar el historial de compras: " + e.getMessage());
        }
        return lista;
    }
}
