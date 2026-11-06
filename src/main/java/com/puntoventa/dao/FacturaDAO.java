package com.puntoventa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.puntoventa.database.ConexionBD;
import modelo.FacturaDatos;
import modelo.LineaVenta;

/** Lee de la BD todo lo necesario para imprimir o reimprimir una factura. */
public class FacturaDAO {

    public FacturaDatos obtener(String numeroFactura) throws Exception {
        Connection con = ConexionBD.getConexion();
        FacturaDatos f = new FacturaDatos();

        String sql = "SELECT v.numero_factura, v.fecha, v.subtotal, v.descuento, v.impuesto, v.total, "
                + "v.efectivo_recibido, v.cambio, v.estado, c.nombre AS cliente, c.cedula, "
                + "e.nombre AS cajero, p.metodo "
                + "FROM ventas v "
                + "JOIN clientes c ON c.cedula = v.cedula_cliente "
                + "JOIN empleados e ON e.id_empleado = v.id_empleado "
                + "LEFT JOIN pagos p ON p.id_pago = v.id_pago "
                + "WHERE v.numero_factura = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, numeroFactura);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new Exception("No existe la factura " + numeroFactura + ".");
                }
                f.numeroFactura = rs.getString("numero_factura");
                f.fecha = rs.getTimestamp("fecha").toLocalDateTime();
                f.subtotal = rs.getDouble("subtotal");
                f.descuento = rs.getDouble("descuento");
                f.impuesto = rs.getDouble("impuesto");
                f.total = rs.getDouble("total");
                double recibido = rs.getDouble("efectivo_recibido");
                f.efectivoRecibido = rs.wasNull() ? null : recibido;
                double cambio = rs.getDouble("cambio");
                f.cambio = rs.wasNull() ? null : cambio;
                f.estado = rs.getString("estado");
                f.clienteNombre = rs.getString("cliente");
                f.clienteCedula = rs.getString("cedula");
                f.cajero = rs.getString("cajero");
                f.metodoPago = rs.getString("metodo");
            }
        }

        String sqlDetalle = "SELECT d.codigo_producto, pr.nombre, d.cantidad, d.precio_unitario "
                + "FROM detalle_venta d JOIN productos pr ON pr.codigo = d.codigo_producto "
                + "WHERE d.numero_factura = ? ORDER BY d.id_detalle";
        try (PreparedStatement stmt = con.prepareStatement(sqlDetalle)) {
            stmt.setString(1, numeroFactura);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    f.lineas.add(new LineaVenta(rs.getString("codigo_producto"), rs.getString("nombre"),
                            rs.getInt("cantidad"), rs.getDouble("precio_unitario")));
                }
            }
        }

        f.empresaNombre = ConfiguracionDAO.obtener(con, "empresa_nombre", "Mi negocio");
        f.empresaRnc = ConfiguracionDAO.obtener(con, "empresa_rnc", "");
        f.empresaDireccion = ConfiguracionDAO.obtener(con, "empresa_direccion", "");
        f.empresaTelefono = ConfiguracionDAO.obtener(con, "empresa_telefono", "");
        return f;
    }
}
