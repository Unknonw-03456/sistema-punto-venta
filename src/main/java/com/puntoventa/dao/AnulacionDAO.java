package com.puntoventa.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.puntoventa.Roles;
import com.puntoventa.database.ConexionBD;
import modelo.VentaResumen;

/**
 * Anulación de ventas SIN borrar historial: la venta queda con estado 'ANULADA', se devuelve el
 * stock, se registra el movimiento de inventario, el reintegro en caja (si fue efectivo) y la auditoría.
 * Solo Administrador y Supervisor pueden anular (se valida aquí, no solo en la pantalla).
 */
public class AnulacionDAO {

    public static boolean puedeAnular(String rol) {
        return Roles.ADMINISTRADOR.equals(rol) || Roles.SUPERVISOR.equals(rol);
    }

    public void anular(int idUsuario, String rol, String numeroFactura, String motivo) throws Exception {
        if (!puedeAnular(rol)) {
            throw new Exception("No tienes permiso para anular ventas.");
        }
        if (motivo == null || motivo.trim().length() < 5) {
            throw new Exception("Indica el motivo de la anulación (mínimo 5 caracteres).");
        }
        String motivoLimpio = motivo.trim();
        if (motivoLimpio.length() > 250) {
            motivoLimpio = motivoLimpio.substring(0, 250);
        }

        Connection con = ConexionBD.getConexion();
        boolean autoPrevio = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int idEmpleado = EmpleadoDAO.obtenerIdEmpleado(con, idUsuario);

            BigDecimal total;
            boolean efectivo;
            Integer idCajaVenta;
            try (PreparedStatement stmt = con.prepareStatement(
                    "SELECT v.total, v.estado, v.id_caja, p.metodo FROM ventas v "
                            + "LEFT JOIN pagos p ON p.id_pago = v.id_pago WHERE v.numero_factura = ? FOR UPDATE")) {
                stmt.setString(1, numeroFactura);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new Exception("La factura " + numeroFactura + " no existe.");
                    }
                    if ("ANULADA".equals(rs.getString("estado"))) {
                        throw new Exception("La factura " + numeroFactura + " ya está anulada.");
                    }
                    total = rs.getBigDecimal("total");
                    efectivo = "Efectivo".equals(rs.getString("metodo"));
                    int c = rs.getInt("id_caja");
                    idCajaVenta = rs.wasNull() ? null : c;
                }
            }

            // Devolver stock de cada producto vendido.
            List<String[]> items = new ArrayList<>();
            try (PreparedStatement stmt = con.prepareStatement(
                    "SELECT codigo_producto, cantidad FROM detalle_venta WHERE numero_factura = ? ORDER BY codigo_producto")) {
                stmt.setString(1, numeroFactura);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        items.add(new String[] {rs.getString(1), String.valueOf(rs.getInt(2))});
                    }
                }
            }
            for (String[] it : items) {
                int cant = Integer.parseInt(it[1]);
                try (PreparedStatement stmt = con.prepareStatement(
                        "UPDATE productos SET stock = stock + ? WHERE codigo = ?")) {
                    stmt.setInt(1, cant);
                    stmt.setString(2, it[0]);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = con.prepareStatement(
                        "INSERT INTO movimientos_inventario (codigo_producto, fecha, tipo, cantidad, referencia, id_empleado) "
                                + "VALUES (?, NOW(), 'ANULACION', ?, ?, ?)")) {
                    stmt.setString(1, it[0]);
                    stmt.setInt(2, cant);
                    stmt.setString(3, numeroFactura);
                    stmt.setInt(4, idEmpleado);
                    stmt.executeUpdate();
                }
            }

            // Reintegro en efectivo: sale de la caja de la venta si sigue abierta; si no, de la caja abierta de quien anula.
            if (efectivo) {
                Integer idCajaDestino = null;
                if (idCajaVenta != null && cajaAbierta(con, idCajaVenta)) {
                    idCajaDestino = idCajaVenta;
                } else {
                    try (PreparedStatement stmt = con.prepareStatement(
                            "SELECT id_caja FROM cajas WHERE id_empleado = ? AND estado = 'ABIERTA' LIMIT 1")) {
                        stmt.setInt(1, idEmpleado);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                idCajaDestino = rs.getInt(1);
                            }
                        }
                    }
                }
                if (idCajaDestino == null) {
                    throw new Exception("La venta fue en efectivo: abre tu caja para registrar el reintegro.");
                }
                try (PreparedStatement stmt = con.prepareStatement(
                        "INSERT INTO movimientos_caja (id_caja, fecha, tipo, monto, referencia, id_empleado) "
                                + "VALUES (?, NOW(), 'ANULACION', ?, ?, ?)")) {
                    stmt.setInt(1, idCajaDestino);
                    stmt.setBigDecimal(2, total.negate());
                    stmt.setString(3, numeroFactura);
                    stmt.setInt(4, idEmpleado);
                    stmt.executeUpdate();
                }
            }

            try (PreparedStatement stmt = con.prepareStatement(
                    "UPDATE ventas SET estado = 'ANULADA', motivo_anulacion = ?, id_empleado_anula = ?, "
                            + "fecha_anulacion = NOW() WHERE numero_factura = ?")) {
                stmt.setString(1, motivoLimpio);
                stmt.setInt(2, idEmpleado);
                stmt.setString(3, numeroFactura);
                stmt.executeUpdate();
            }

            AuditoriaDAO.registrar(con, idUsuario, "ANULACION", "venta", numeroFactura,
                    "Total " + total + " | Motivo: " + motivoLimpio);
            con.commit();
        } catch (Exception e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(autoPrevio);
        }
    }

    private boolean cajaAbierta(Connection con, int idCaja) throws Exception {
        try (PreparedStatement stmt = con.prepareStatement("SELECT estado FROM cajas WHERE id_caja = ?")) {
            stmt.setInt(1, idCaja);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && "ABIERTA".equals(rs.getString(1));
            }
        }
    }

    /** Últimas 200 ventas, más recientes primero. */
    public List<VentaResumen> historial() {
        List<VentaResumen> lista = new ArrayList<>();
        String sql = "SELECT v.numero_factura, v.fecha, c.nombre AS cliente, u.usuario AS cajero, p.metodo, v.total, v.estado "
                + "FROM ventas v JOIN clientes c ON c.cedula = v.cedula_cliente "
                + "JOIN empleados e ON e.id_empleado = v.id_empleado "
                + "JOIN usuarios u ON u.id_usuario = e.id_usuario "
                + "LEFT JOIN pagos p ON p.id_pago = v.id_pago "
                + "ORDER BY v.fecha DESC, v.numero_factura DESC LIMIT 200";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new VentaResumen(rs.getString("numero_factura"), rs.getTimestamp("fecha").toLocalDateTime(),
                        rs.getString("cliente"), rs.getString("cajero"), rs.getString("metodo"),
                        rs.getDouble("total"), rs.getString("estado")));
            }
        } catch (Exception e) {
            System.err.println("Error al cargar el historial de ventas: " + e.getMessage());
        }
        return lista;
    }
}
