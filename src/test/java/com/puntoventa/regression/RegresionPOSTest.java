package com.puntoventa.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.puntoventa.Roles;
import com.puntoventa.dao.AnulacionDAO;
import com.puntoventa.dao.ReportePOSDAO;
import com.puntoventa.service.CalculoFactura;
import com.puntoventa.service.PoliticaDescuento;
import modelo.LineaCompra;
import modelo.LineaVenta;

/**
 * Pruebas de REGRESIÓN: protegen funciones que ya operaban (facturación, permisos, reportes, compras)
 * para que una mejora nueva no las rompa. No necesitan base de datos, así corren también en GitHub Actions.
 */
class RegresionPOSTest {

    private static List<LineaVenta> unaLinea(int cantidad, double precio) {
        List<LineaVenta> lineas = new ArrayList<>();
        lineas.add(new LineaVenta("1235", "tubo cobre", cantidad, precio));
        return lineas;
    }

    @Test
    @DisplayName("RG-01 ITBIS y totales: 1 x 500 = 500 + 90 ITBIS = 590 (igual a FAC-000001)")
    void itbisYTotalesVentaBase() {
        CalculoFactura.Resultado r = CalculoFactura.calcular(unaLinea(1, 500.00), 0, 18, false, null);
        assertEquals(new BigDecimal("500.00"), r.subtotal);
        assertEquals(new BigDecimal("90.00"), r.itbis);
        assertEquals(new BigDecimal("591.00"), r.total);
    }

    @Test
    @DisplayName("RG-02 Descuento no rompe el ITBIS: 2 x 500, 10% desc -> base 900, ITBIS 162, total 1062")
    void descuentoConservaCalculoDeItbis() {
        CalculoFactura.Resultado r = CalculoFactura.calcular(unaLinea(2, 500.00), 10, 18, false, null);
        assertEquals(new BigDecimal("100.00"), r.descuento);
        assertEquals(new BigDecimal("900.00"), r.baseImponible);
        assertEquals(new BigDecimal("162.00"), r.itbis);
        assertEquals(new BigDecimal("1062.00"), r.total);
    }

    @Test
    @DisplayName("RG-03 Efectivo: cambio correcto y rechazo si el efectivo no alcanza")
    void efectivoYCambio() {
        CalculoFactura.Resultado r = CalculoFactura.calcular(unaLinea(1, 500.00), 0, 18, true, 600.0);
        assertEquals(new BigDecimal("10.00"), r.cambio);
        assertThrows(IllegalArgumentException.class,
                () -> CalculoFactura.calcular(unaLinea(1, 500.00), 0, 18, true, 100.0));
    }

    @Test
    @DisplayName("RG-04 Permisos de anulación: solo Administrador y Supervisor")
    void permisosDeAnulacion() {
        assertTrue(AnulacionDAO.puedeAnular(Roles.ADMINISTRADOR));
        assertTrue(AnulacionDAO.puedeAnular(Roles.SUPERVISOR));
        assertFalse(AnulacionDAO.puedeAnular(Roles.CAJERO));
        assertFalse(AnulacionDAO.puedeAnular(Roles.ENCARGADO_INVENTARIO));
        assertFalse(AnulacionDAO.puedeAnular(null));
    }

    @Test
    @DisplayName("RG-05 Reportes por rol: el Cajero no ve inventario ni ventas por cajero")
    void reportesPorRol() {
        List<String> cajero = ReportePOSDAO.disponiblesPara(Roles.CAJERO);
        assertTrue(cajero.contains(ReportePOSDAO.VENTAS_FECHA));
        assertFalse(cajero.contains(ReportePOSDAO.VENTAS_CAJERO));
        assertFalse(cajero.contains(ReportePOSDAO.INVENTARIO));

        List<String> encargado = ReportePOSDAO.disponiblesPara(Roles.ENCARGADO_INVENTARIO);
        assertTrue(encargado.contains(ReportePOSDAO.INVENTARIO));
        assertFalse(encargado.contains(ReportePOSDAO.VENTAS_FECHA));

        assertEquals(6, ReportePOSDAO.disponiblesPara(Roles.ADMINISTRADOR).size());
    }

    @Test
    @DisplayName("RG-06 Compras: el subtotal de una línea es cantidad x costo")
    void subtotalDeCompra() {
        LineaCompra linea = new LineaCompra("1235", "tubo cobre", 3, 12.50);
        assertEquals(37.50, linea.getSubtotal(), 0.001);
        linea.setCantidad(4);
        assertEquals(50.00, linea.getSubtotal(), 0.001);
    }

    @Test
    @DisplayName("RG-07 Mejora nueva: el Cajero no puede dar más de 10% de descuento")
    void descuentoMaximoPorRol() {
        PoliticaDescuento.validar(Roles.CAJERO, 10);
        assertThrows(IllegalArgumentException.class, () -> PoliticaDescuento.validar(Roles.CAJERO, 10.5));
        PoliticaDescuento.validar(Roles.SUPERVISOR, 50);
        PoliticaDescuento.validar(Roles.ADMINISTRADOR, 100);
        assertThrows(IllegalArgumentException.class, () -> PoliticaDescuento.validar(Roles.ENCARGADO_INVENTARIO, 1));
        assertThrows(IllegalArgumentException.class, () -> PoliticaDescuento.validar(null, 5));
    }

    @Test
    @DisplayName("RG-08 La mejora no altera el cálculo: con 10% el total sigue siendo 1062")
    void mejoraNoAlteraCalculo() {
        PoliticaDescuento.validar(Roles.CAJERO, 10);
        CalculoFactura.Resultado r = CalculoFactura.calcular(unaLinea(2, 500.00), 10, 18, false, null);
        assertEquals(new BigDecimal("1062.00"), r.total);
    }
}
