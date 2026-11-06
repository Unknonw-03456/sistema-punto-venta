package com.puntoventa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.puntoventa.Roles;
import com.puntoventa.database.ConexionBD;
import modelo.ReporteTabla;

/**
 * Reportes del POS construidos con datos reales. Las ventas ANULADAS no suman a los totales.
 * Un Cajero solo ve sus propias ventas (se filtra aquí, no solo en la pantalla).
 */
public class ReportePOSDAO {

    public static final String VENTAS_FECHA = "Ventas por fecha";
    public static final String VENTAS_CAJERO = "Ventas por cajero";
    public static final String VENTAS_PRODUCTO = "Ventas por producto";
    public static final String INVENTARIO = "Inventario actual";
    public static final String MOVIMIENTOS = "Movimientos de inventario";
    public static final String COMPRAS = "Compras a proveedores";

    /** Reportes que cada rol puede emitir. */
    public static List<String> disponiblesPara(String rol) {
        if (Roles.CAJERO.equals(rol)) {
            return Arrays.asList(VENTAS_FECHA, VENTAS_PRODUCTO);
        }
        if (Roles.ENCARGADO_INVENTARIO.equals(rol)) {
            return Arrays.asList(INVENTARIO, MOVIMIENTOS, COMPRAS);
        }
        return Arrays.asList(VENTAS_FECHA, VENTAS_CAJERO, VENTAS_PRODUCTO, INVENTARIO, MOVIMIENTOS, COMPRAS);
    }

    public ReporteTabla generar(String tipo, LocalDate desde, LocalDate hasta, int idUsuario, String rol) throws Exception {
        if (!disponiblesPara(rol).contains(tipo)) {
            throw new Exception("No tienes permiso para emitir este reporte.");
        }
        if (desde == null || hasta == null) {
            throw new Exception("Selecciona las fechas.");
        }
        if (hasta.isBefore(desde)) {
            throw new Exception("La fecha final no puede ser anterior a la inicial.");
        }
        Timestamp d = Timestamp.valueOf(desde.atStartOfDay());
        Timestamp h = Timestamp.valueOf(hasta.plusDays(1).atStartOfDay());
        Integer soloUsuario = Roles.CAJERO.equals(rol) ? idUsuario : null;
        String periodo = "Del " + desde + " al " + hasta;
        Connection con = ConexionBD.getConexion();

        switch (tipo) {
            case VENTAS_FECHA: return ventasPorFecha(con, d, h, soloUsuario, periodo);
            case VENTAS_CAJERO: return ventasPorCajero(con, d, h, periodo);
            case VENTAS_PRODUCTO: return ventasPorProducto(con, d, h, soloUsuario, periodo);
            case INVENTARIO: return inventario(con);
            case MOVIMIENTOS: return movimientos(con, d, h, periodo);
            case COMPRAS: return compras(con, d, h, periodo);
            default: throw new Exception("Reporte desconocido.");
        }
    }

    private ReporteTabla ventasPorFecha(Connection con, Timestamp d, Timestamp h, Integer usuario, String periodo) throws Exception {
        String sql = "SELECT DATE(v.fecha) f, COUNT(*) n, SUM(v.subtotal) st, SUM(v.descuento) ds, SUM(v.impuesto) im, SUM(v.total) t "
                + "FROM ventas v JOIN empleados e ON e.id_empleado = v.id_empleado "
                + "WHERE v.estado = 'EMITIDA' AND v.fecha >= ? AND v.fecha < ? "
                + (usuario != null ? "AND e.id_usuario = ? " : "")
                + "GROUP BY DATE(v.fecha) ORDER BY f";
        List<String[]> filas = new ArrayList<>();
        double total = 0;
        int facturas = 0;
        try (PreparedStatement st = con.prepareStatement(sql)) {
            st.setTimestamp(1, d);
            st.setTimestamp(2, h);
            if (usuario != null) st.setInt(3, usuario);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[] {rs.getString("f"), String.valueOf(rs.getInt("n")), m(rs.getDouble("st")),
                            m(rs.getDouble("ds")), m(rs.getDouble("im")), m(rs.getDouble("t"))});
                    total += rs.getDouble("t");
                    facturas += rs.getInt("n");
                }
            }
        }
        return new ReporteTabla(VENTAS_FECHA, periodo,
                Arrays.asList("Fecha", "Facturas", "Subtotal", "Descuento", "ITBIS", "Total"), filas,
                facturas + " factura(s) | Total vendido: $" + m(total));
    }

    private ReporteTabla ventasPorCajero(Connection con, Timestamp d, Timestamp h, String periodo) throws Exception {
        String sql = "SELECT u.usuario, SUM(v.estado = 'EMITIDA') n, SUM(v.estado = 'ANULADA') a, "
                + "SUM(CASE WHEN v.estado = 'EMITIDA' THEN v.total ELSE 0 END) t "
                + "FROM ventas v JOIN empleados e ON e.id_empleado = v.id_empleado "
                + "JOIN usuarios u ON u.id_usuario = e.id_usuario "
                + "WHERE v.fecha >= ? AND v.fecha < ? GROUP BY u.usuario ORDER BY t DESC";
        List<String[]> filas = new ArrayList<>();
        double total = 0;
        try (PreparedStatement st = con.prepareStatement(sql)) {
            st.setTimestamp(1, d);
            st.setTimestamp(2, h);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[] {rs.getString("usuario"), String.valueOf(rs.getInt("n")),
                            String.valueOf(rs.getInt("a")), m(rs.getDouble("t"))});
                    total += rs.getDouble("t");
                }
            }
        }
        return new ReporteTabla(VENTAS_CAJERO, periodo,
                Arrays.asList("Cajero", "Facturas emitidas", "Anuladas", "Total vendido"), filas,
                "Total general: $" + m(total));
    }

    private ReporteTabla ventasPorProducto(Connection con, Timestamp d, Timestamp h, Integer usuario, String periodo) throws Exception {
        String sql = "SELECT pr.codigo, pr.nombre, SUM(dv.cantidad) u, SUM(dv.cantidad * dv.precio_unitario) t, "
                + "SUM(dv.cantidad * (dv.precio_unitario - dv.costo_unitario)) g "
                + "FROM detalle_venta dv JOIN ventas v ON v.numero_factura = dv.numero_factura "
                + "JOIN productos pr ON pr.codigo = dv.codigo_producto "
                + "JOIN empleados e ON e.id_empleado = v.id_empleado "
                + "WHERE v.estado = 'EMITIDA' AND v.fecha >= ? AND v.fecha < ? "
                + (usuario != null ? "AND e.id_usuario = ? " : "")
                + "GROUP BY pr.codigo, pr.nombre ORDER BY u DESC";
        List<String[]> filas = new ArrayList<>();
        double total = 0;
        int unidades = 0;
        try (PreparedStatement st = con.prepareStatement(sql)) {
            st.setTimestamp(1, d);
            st.setTimestamp(2, h);
            if (usuario != null) st.setInt(3, usuario);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[] {rs.getString("codigo"), rs.getString("nombre"),
                            String.valueOf(rs.getInt("u")), m(rs.getDouble("t")), m(rs.getDouble("g"))});
                    total += rs.getDouble("t");
                    unidades += rs.getInt("u");
                }
            }
        }
        return new ReporteTabla(VENTAS_PRODUCTO, periodo,
                Arrays.asList("Código", "Producto", "Unidades", "Vendido (sin ITBIS)", "Ganancia bruta"), filas,
                unidades + " unidad(es) | Vendido: $" + m(total));
    }

    private ReporteTabla inventario(Connection con) throws Exception {
        String sql = "SELECT codigo, nombre, stock, costo, precio, stock * costo AS valor FROM productos WHERE activo = 1 ORDER BY nombre";
        List<String[]> filas = new ArrayList<>();
        double valor = 0;
        int bajos = 0;
        try (PreparedStatement st = con.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                int stock = rs.getInt("stock");
                if (stock < 10) bajos++;
                filas.add(new String[] {rs.getString("codigo"), rs.getString("nombre"), String.valueOf(stock),
                        m(rs.getDouble("costo")), m(rs.getDouble("precio")), m(rs.getDouble("valor"))});
                valor += rs.getDouble("valor");
            }
        }
        return new ReporteTabla(INVENTARIO, "Al " + LocalDate.now(),
                Arrays.asList("Código", "Producto", "Stock", "Costo", "Precio", "Valor en inventario"), filas,
                "Valor total: $" + m(valor) + " | Productos con stock bajo (<10): " + bajos);
    }

    private ReporteTabla movimientos(Connection con, Timestamp d, Timestamp h, String periodo) throws Exception {
        String sql = "SELECT mi.fecha, pr.nombre, mi.tipo, mi.cantidad, mi.referencia, u.usuario "
                + "FROM movimientos_inventario mi JOIN productos pr ON pr.codigo = mi.codigo_producto "
                + "JOIN empleados e ON e.id_empleado = mi.id_empleado JOIN usuarios u ON u.id_usuario = e.id_usuario "
                + "WHERE mi.fecha >= ? AND mi.fecha < ? ORDER BY mi.fecha DESC LIMIT 1000";
        List<String[]> filas = new ArrayList<>();
        try (PreparedStatement st = con.prepareStatement(sql)) {
            st.setTimestamp(1, d);
            st.setTimestamp(2, h);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String f = rs.getTimestamp("fecha").toString();
                    filas.add(new String[] {f.substring(0, 16), rs.getString("nombre"), rs.getString("tipo"),
                            String.valueOf(rs.getInt("cantidad")), rs.getString("referencia"), rs.getString("usuario")});
                }
            }
        }
        return new ReporteTabla(MOVIMIENTOS, periodo,
                Arrays.asList("Fecha", "Producto", "Tipo", "Cantidad", "Referencia", "Usuario"), filas,
                filas.size() + " movimiento(s) (máx. 1000)");
    }

    private ReporteTabla compras(Connection con, Timestamp d, Timestamp h, String periodo) throws Exception {
        String sql = "SELECT c.id_compra, c.fecha, pv.nombre proveedor, c.numero_documento, u.usuario, c.total "
                + "FROM compras c JOIN proveedores pv ON pv.id_proveedor = c.id_proveedor "
                + "JOIN empleados e ON e.id_empleado = c.id_empleado JOIN usuarios u ON u.id_usuario = e.id_usuario "
                + "WHERE c.fecha >= ? AND c.fecha < ? ORDER BY c.fecha DESC";
        List<String[]> filas = new ArrayList<>();
        double total = 0;
        try (PreparedStatement st = con.prepareStatement(sql)) {
            st.setTimestamp(1, d);
            st.setTimestamp(2, h);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String doc = rs.getString("numero_documento");
                    filas.add(new String[] {"COMP-" + rs.getInt("id_compra"), rs.getTimestamp("fecha").toString().substring(0, 16),
                            rs.getString("proveedor"), doc == null ? "-" : doc, rs.getString("usuario"), m(rs.getDouble("total"))});
                    total += rs.getDouble("total");
                }
            }
        }
        return new ReporteTabla(COMPRAS, periodo,
                Arrays.asList("N.º", "Fecha", "Proveedor", "Documento", "Registrado por", "Total"), filas,
                filas.size() + " compra(s) | Total comprado: $" + m(total));
    }

    private static String m(double v) {
        return String.format("%,.2f", v);
    }
}
