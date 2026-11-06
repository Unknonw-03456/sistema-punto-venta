package com.puntoventa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.puntoventa.database.ConexionBD;
import modelo.CajaInfo;
import modelo.ResumenCaja;

/**
 * Apertura, cierre y arqueo de caja. Cada cajero puede tener UNA caja abierta a la vez.
 * En "movimientos_caja" solo se registra el EFECTIVO (ventas en efectivo, anulaciones,
 * ingresos y egresos); las ventas con tarjeta o transferencia se resumen desde "ventas".
 */
public class CajaDAO {

    /** Devuelve la caja abierta del usuario, o null si no tiene ninguna. */
    public CajaInfo obtenerAbierta(int idUsuario) {
        String sql = "SELECT c.id_caja, u.usuario, c.fecha_apertura, c.monto_inicial, c.estado "
                + "FROM cajas c JOIN empleados e ON e.id_empleado = c.id_empleado "
                + "JOIN usuarios u ON u.id_usuario = e.id_usuario "
                + "WHERE e.id_usuario = ? AND c.estado = 'ABIERTA' LIMIT 1";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CajaInfo(rs.getInt("id_caja"), rs.getString("usuario"),
                            rs.getTimestamp("fecha_apertura").toLocalDateTime(),
                            rs.getDouble("monto_inicial"), null, null, null, null, rs.getString("estado"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error al consultar la caja abierta: " + e.getMessage());
        }
        return null;
    }

    public int abrir(int idUsuario, double montoInicial) throws Exception {
        if (Double.isNaN(montoInicial) || montoInicial < 0) {
            throw new Exception("El monto inicial no puede ser negativo.");
        }
        Connection con = ConexionBD.getConexion();
        boolean autoPrevio = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int idEmpleado = EmpleadoDAO.obtenerIdEmpleado(con, idUsuario);

            try (PreparedStatement stmt = con.prepareStatement(
                    "SELECT 1 FROM cajas WHERE id_empleado = ? AND estado = 'ABIERTA' FOR UPDATE")) {
                stmt.setInt(1, idEmpleado);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        throw new Exception("Ya tienes una caja abierta. Ciérrala antes de abrir otra.");
                    }
                }
            }

            int idCaja;
            try (PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO cajas (id_empleado, fecha_apertura, monto_inicial, estado) VALUES (?, NOW(), ?, 'ABIERTA')",
                    Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, idEmpleado);
                stmt.setDouble(2, montoInicial);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    idCaja = keys.getInt(1);
                }
            }

            AuditoriaDAO.registrar(con, idUsuario, "APERTURA_CAJA", "caja", String.valueOf(idCaja),
                    "Monto inicial: " + String.format("%.2f", montoInicial));
            con.commit();
            return idCaja;
        } catch (Exception e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(autoPrevio);
        }
    }

    /** Efectivo esperado y ventas por método de pago de una caja (abierta o cerrada). */
    public ResumenCaja resumen(int idCaja) throws Exception {
        Connection con = ConexionBD.getConexion();
        return resumen(con, idCaja);
    }

    private ResumenCaja resumen(Connection con, int idCaja) throws Exception {
        double inicial;
        try (PreparedStatement stmt = con.prepareStatement("SELECT monto_inicial FROM cajas WHERE id_caja = ?")) {
            stmt.setInt(1, idCaja);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new Exception("La caja no existe.");
                }
                inicial = rs.getDouble("monto_inicial");
            }
        }

        double movimientos = 0;
        try (PreparedStatement stmt = con.prepareStatement(
                "SELECT COALESCE(SUM(monto), 0) AS total FROM movimientos_caja WHERE id_caja = ?")) {
            stmt.setInt(1, idCaja);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    movimientos = rs.getDouble("total");
                }
            }
        }

        ResumenCaja resumen = new ResumenCaja(inicial, movimientos);
        String sql = "SELECT p.metodo, COUNT(*) AS cantidad, COALESCE(SUM(v.total), 0) AS total "
                + "FROM ventas v JOIN pagos p ON p.id_pago = v.id_pago "
                + "WHERE v.id_caja = ? AND v.estado = 'EMITIDA' GROUP BY p.metodo ORDER BY p.metodo";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idCaja);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    resumen.agregarMetodo(rs.getString("metodo"), rs.getInt("cantidad"), rs.getDouble("total"));
                }
            }
        }
        return resumen;
    }

    /**
     * Cierra la caja: compara el efectivo contado con el esperado y guarda la diferencia
     * (negativa = faltante, positiva = sobrante). Devuelve la diferencia.
     */
    public double cerrar(int idCaja, int idUsuario, double montoContado, String observacion) throws Exception {
        if (Double.isNaN(montoContado) || montoContado < 0) {
            throw new Exception("El monto contado no puede ser negativo.");
        }
        Connection con = ConexionBD.getConexion();
        boolean autoPrevio = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int idEmpleado = EmpleadoDAO.obtenerIdEmpleado(con, idUsuario);

            try (PreparedStatement stmt = con.prepareStatement(
                    "SELECT id_empleado, estado FROM cajas WHERE id_caja = ? FOR UPDATE")) {
                stmt.setInt(1, idCaja);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new Exception("La caja no existe.");
                    }
                    if (!"ABIERTA".equals(rs.getString("estado"))) {
                        throw new Exception("Esta caja ya está cerrada.");
                    }
                    if (rs.getInt("id_empleado") != idEmpleado) {
                        throw new Exception("Solo quien abrió la caja puede cerrarla.");
                    }
                }
            }

            double esperado = resumen(con, idCaja).getMontoEsperado();
            double diferencia = Math.round((montoContado - esperado) * 100.0) / 100.0;

            try (PreparedStatement stmt = con.prepareStatement(
                    "UPDATE cajas SET fecha_cierre = NOW(), monto_esperado = ?, monto_contado = ?, "
                            + "diferencia = ?, estado = 'CERRADA', observacion = ? WHERE id_caja = ?")) {
                stmt.setDouble(1, esperado);
                stmt.setDouble(2, montoContado);
                stmt.setDouble(3, diferencia);
                stmt.setString(4, observacion == null || observacion.isBlank() ? null : observacion.trim());
                stmt.setInt(5, idCaja);
                stmt.executeUpdate();
            }

            AuditoriaDAO.registrar(con, idUsuario, "CIERRE_CAJA", "caja", String.valueOf(idCaja),
                    "Esperado: " + String.format("%.2f", esperado) + " | Contado: "
                            + String.format("%.2f", montoContado) + " | Diferencia: " + String.format("%.2f", diferencia));
            con.commit();
            return diferencia;
        } catch (Exception e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(autoPrevio);
        }
    }

    /** Historial de cajas. El administrador ve todas; los demás solo las suyas. */
    public List<CajaInfo> historial(int idUsuario, boolean verTodas) {
        List<CajaInfo> lista = new ArrayList<>();
        String sql = "SELECT c.id_caja, u.usuario, c.fecha_apertura, c.monto_inicial, c.fecha_cierre, "
                + "c.monto_esperado, c.monto_contado, c.diferencia, c.estado "
                + "FROM cajas c JOIN empleados e ON e.id_empleado = c.id_empleado "
                + "JOIN usuarios u ON u.id_usuario = e.id_usuario "
                + (verTodas ? "" : "WHERE e.id_usuario = ? ")
                + "ORDER BY c.id_caja DESC LIMIT 50";
        try (PreparedStatement stmt = ConexionBD.getConexion().prepareStatement(sql)) {
            if (!verTodas) {
                stmt.setInt(1, idUsuario);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp cierre = rs.getTimestamp("fecha_cierre");
                    lista.add(new CajaInfo(
                            rs.getInt("id_caja"), rs.getString("usuario"),
                            rs.getTimestamp("fecha_apertura").toLocalDateTime(),
                            rs.getDouble("monto_inicial"),
                            cierre == null ? null : cierre.toLocalDateTime(),
                            nullableDouble(rs, "monto_esperado"), nullableDouble(rs, "monto_contado"),
                            nullableDouble(rs, "diferencia"), rs.getString("estado")));
                }
            }
        } catch (Exception e) {
            System.err.println("Error al cargar el historial de cajas: " + e.getMessage());
        }
        return lista;
    }

    private static Double nullableDouble(ResultSet rs, String columna) throws Exception {
        double valor = rs.getDouble(columna);
        return rs.wasNull() ? null : valor;
    }
}
