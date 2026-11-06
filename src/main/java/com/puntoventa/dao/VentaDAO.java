package com.puntoventa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.puntoventa.database.ConexionBD;
import com.puntoventa.service.CalculoFactura;
import modelo.LineaVenta;

/**
 * Registro de ventas. Todo ocurre en UNA transacción: si cualquier paso falla (por ejemplo
 * no alcanza el stock), se revierte todo y no queda nada a medias.
 */
public class VentaDAO {

    public static final String CLIENTE_GENERICO = "000-0000000-0";

    /** Lo que se devuelve después de registrar una venta. */
    public static class ResultadoVenta {
        public final String numeroFactura;
        public final CalculoFactura.Resultado totales;

        ResultadoVenta(String numeroFactura, CalculoFactura.Resultado totales) {
            this.numeroFactura = numeroFactura;
            this.totales = totales;
        }
    }

    /**
     * @param idUsuario      usuario que vende (de la sesión)
     * @param idCaja         caja abierta de ese usuario
     * @param cedulaCliente  cédula del cliente; vacío = consumidor final
     * @param carrito        productos y cantidades (los precios se vuelven a leer de la BD)
     * @param descuentoPct   descuento en porcentaje (0 a 100)
     * @param metodoPago     Efectivo, Tarjeta o Transferencia
     * @param efectivoRecibido monto entregado (solo se usa si es Efectivo)
     */
    public ResultadoVenta registrar(int idUsuario, int idCaja, String cedulaCliente, List<LineaVenta> carrito,
                                    double descuentoPct, String metodoPago, Double efectivoRecibido) throws Exception {
        if (carrito == null || carrito.isEmpty()) {
            throw new Exception("El carrito está vacío.");
        }
        if (metodoPago == null || metodoPago.isBlank()) {
            throw new Exception("Selecciona el método de pago.");
        }
        boolean esEfectivo = "Efectivo".equals(metodoPago);
        String cedula = (cedulaCliente == null || cedulaCliente.isBlank()) ? CLIENTE_GENERICO : cedulaCliente;

        Connection con = ConexionBD.getConexion();
        boolean autoPrevio = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int idEmpleado = EmpleadoDAO.obtenerIdEmpleado(con, idUsuario);
            verificarCajaAbierta(con, idCaja, idEmpleado);

            // Se juntan las cantidades por producto (ordenadas por código para evitar bloqueos cruzados).
            Map<String, Integer> cantidades = new TreeMap<>();
            for (LineaVenta linea : carrito) {
                cantidades.merge(linea.getCodigo(), linea.getCantidad(), Integer::sum);
            }

            // Se leen precio, costo y stock reales de la BD y se bloquean las filas hasta el commit.
            List<LineaVenta> lineas = new ArrayList<>();
            Map<String, Double> costos = new TreeMap<>();
            for (Map.Entry<String, Integer> entrada : cantidades.entrySet()) {
                String codigo = entrada.getKey();
                int cantidad = entrada.getValue();
                try (PreparedStatement stmt = con.prepareStatement(
                        "SELECT nombre, precio, costo, stock, activo FROM productos WHERE codigo = ? FOR UPDATE")) {
                    stmt.setString(1, codigo);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new Exception("El producto " + codigo + " ya no existe.");
                        }
                        String nombre = rs.getString("nombre");
                        if (!rs.getBoolean("activo")) {
                            throw new Exception("El producto \"" + nombre + "\" está desactivado.");
                        }
                        if (cantidad <= 0) {
                            throw new Exception("La cantidad de \"" + nombre + "\" debe ser mayor a 0.");
                        }
                        int stock = rs.getInt("stock");
                        if (stock < cantidad) {
                            throw new Exception("Stock insuficiente de \"" + nombre + "\": disponible " + stock
                                    + ", solicitado " + cantidad + ".");
                        }
                        lineas.add(new LineaVenta(codigo, nombre, cantidad, rs.getDouble("precio")));
                        costos.put(codigo, rs.getDouble("costo"));
                    }
                }
            }

            double itbisPct = ConfiguracionDAO.obtenerNumero(con, "itbis_porcentaje", 18);
            CalculoFactura.Resultado t;
            try {
                t = CalculoFactura.calcular(lineas, descuentoPct, itbisPct, esEfectivo, efectivoRecibido);
            } catch (IllegalArgumentException e) {
                throw new Exception(e.getMessage());
            }

            String numeroFactura = siguienteNumeroFactura(con);

            int idPago;
            try (PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO pagos (monto, metodo) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                stmt.setBigDecimal(1, t.total);
                stmt.setString(2, metodoPago);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    idPago = keys.getInt(1);
                }
            }

            try (PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO ventas (numero_factura, fecha, total, cedula_cliente, id_empleado, id_pago, "
                            + "subtotal, descuento, impuesto, efectivo_recibido, cambio, estado, id_caja) "
                            + "VALUES (?, NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, 'EMITIDA', ?)")) {
                stmt.setString(1, numeroFactura);
                stmt.setBigDecimal(2, t.total);
                stmt.setString(3, cedula);
                stmt.setInt(4, idEmpleado);
                stmt.setInt(5, idPago);
                stmt.setBigDecimal(6, t.subtotal);
                stmt.setBigDecimal(7, t.descuento);
                stmt.setBigDecimal(8, t.itbis);
                if (esEfectivo) {
                    stmt.setDouble(9, efectivoRecibido);
                    stmt.setBigDecimal(10, t.cambio);
                } else {
                    stmt.setNull(9, java.sql.Types.DECIMAL);
                    stmt.setNull(10, java.sql.Types.DECIMAL);
                }
                stmt.setInt(11, idCaja);
                stmt.executeUpdate();
            }

            for (LineaVenta linea : lineas) {
                try (PreparedStatement stmt = con.prepareStatement(
                        "INSERT INTO detalle_venta (numero_factura, codigo_producto, cantidad, precio_unitario, costo_unitario) "
                                + "VALUES (?, ?, ?, ?, ?)")) {
                    stmt.setString(1, numeroFactura);
                    stmt.setString(2, linea.getCodigo());
                    stmt.setInt(3, linea.getCantidad());
                    stmt.setDouble(4, linea.getPrecioUnitario());
                    stmt.setDouble(5, costos.get(linea.getCodigo()));
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = con.prepareStatement(
                        "UPDATE productos SET stock = stock - ? WHERE codigo = ?")) {
                    stmt.setInt(1, linea.getCantidad());
                    stmt.setString(2, linea.getCodigo());
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = con.prepareStatement(
                        "INSERT INTO movimientos_inventario (codigo_producto, fecha, tipo, cantidad, referencia, id_empleado) "
                                + "VALUES (?, NOW(), 'VENTA', ?, ?, ?)")) {
                    stmt.setString(1, linea.getCodigo());
                    stmt.setInt(2, -linea.getCantidad());
                    stmt.setString(3, numeroFactura);
                    stmt.setInt(4, idEmpleado);
                    stmt.executeUpdate();
                }
            }

            // Solo el efectivo entra al arqueo de caja.
            if (esEfectivo) {
                try (PreparedStatement stmt = con.prepareStatement(
                        "INSERT INTO movimientos_caja (id_caja, fecha, tipo, monto, referencia, id_empleado) "
                                + "VALUES (?, NOW(), 'VENTA', ?, ?, ?)")) {
                    stmt.setInt(1, idCaja);
                    stmt.setBigDecimal(2, t.total);
                    stmt.setString(3, numeroFactura);
                    stmt.setInt(4, idEmpleado);
                    stmt.executeUpdate();
                }
            }

            AuditoriaDAO.registrar(con, idUsuario, "VENTA", "venta", numeroFactura,
                    "Total " + t.total + " | " + metodoPago + " | " + lineas.size() + " producto(s)");

            con.commit();
            return new ResultadoVenta(numeroFactura, t);
        } catch (Exception e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(autoPrevio);
        }
    }

    private void verificarCajaAbierta(Connection con, int idCaja, int idEmpleado) throws Exception {
        try (PreparedStatement stmt = con.prepareStatement(
                "SELECT id_empleado, estado FROM cajas WHERE id_caja = ?")) {
            stmt.setInt(1, idCaja);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next() || !"ABIERTA".equals(rs.getString("estado"))) {
                    throw new Exception("No hay una caja abierta. Abre la caja para poder vender.");
                }
                if (rs.getInt("id_empleado") != idEmpleado) {
                    throw new Exception("Esa caja pertenece a otro cajero.");
                }
            }
        }
    }

    /** Numeración correlativa sin saltos ni repetidos, protegida dentro de la transacción. */
    private String siguienteNumeroFactura(Connection con) throws Exception {
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate("UPDATE secuencias SET ultimo = LAST_INSERT_ID(ultimo + 1) WHERE nombre = 'factura'");
        }
        int numero;
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()")) {
            rs.next();
            numero = rs.getInt(1);
        }
        String prefijo = ConfiguracionDAO.obtener(con, "prefijo_factura", "FAC-");
        return prefijo + String.format("%06d", numero);
    }
}
