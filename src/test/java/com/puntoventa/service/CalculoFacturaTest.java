package com.puntoventa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import modelo.LineaVenta;

class CalculoFacturaTest {

    private static List<LineaVenta> carrito() {
        List<LineaVenta> lineas = new ArrayList<>();
        lineas.add(new LineaVenta("P001", "Compresor", 2, 500.00));
        lineas.add(new LineaVenta("P002", "Filtro", 1, 450.00));
        return lineas;
    }

    @Test
    @DisplayName("Venta sin descuento: subtotal, ITBIS 18% y total")
    void ventaSinDescuento() {
        CalculoFactura.Resultado r = CalculoFactura.calcular(carrito(), 0, 18, false, null);
        assertEquals(new BigDecimal("1450.00"), r.subtotal);
        assertEquals(new BigDecimal("261.00"), r.itbis);
        assertEquals(new BigDecimal("1711.00"), r.total);
    }

    @Test
    @DisplayName("Venta con 10% de descuento pagada en efectivo: calcula descuento, ITBIS y cambio")
    void ventaConDescuentoYCambio() {
        CalculoFactura.Resultado r = CalculoFactura.calcular(carrito(), 10, 18, true, 2000.0);
        assertEquals(new BigDecimal("145.00"), r.descuento);
        assertEquals(new BigDecimal("234.90"), r.itbis);
        assertEquals(new BigDecimal("1539.90"), r.total);
        assertEquals(new BigDecimal("460.10"), r.cambio);
    }

    @Test
    @DisplayName("Límite: pago exacto deja cambio 0 y descuento 100% deja total 0")
    void limites() {
        assertEquals(new BigDecimal("0.00"), CalculoFactura.calcular(carrito(), 0, 18, true, 1711.0).cambio);
        assertEquals(new BigDecimal("0.00"), CalculoFactura.calcular(carrito(), 100, 18, false, null).total);
    }

    @Test
    @DisplayName("El redondeo es a 2 decimales (3 x 0.10 con ITBIS 18% = 0.35)")
    void redondeo() {
        List<LineaVenta> lineas = new ArrayList<>();
        lineas.add(new LineaVenta("X", "Tornillo", 3, 0.10));
        assertEquals(new BigDecimal("0.35"), CalculoFactura.calcular(lineas, 0, 18, false, null).total);
    }

    @Test
    @DisplayName("Inválido: carrito vacío")
    void carritoVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculoFactura.calcular(new ArrayList<>(), 0, 18, false, null));
    }

    @Test
    @DisplayName("Inválido: descuento fuera de 0 a 100")
    void descuentoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> CalculoFactura.calcular(carrito(), 101, 18, false, null));
        assertThrows(IllegalArgumentException.class, () -> CalculoFactura.calcular(carrito(), -1, 18, false, null));
    }

    @Test
    @DisplayName("Inválido: efectivo recibido menor al total o sin indicar")
    void efectivoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> CalculoFactura.calcular(carrito(), 0, 18, true, 1000.0));
        assertThrows(IllegalArgumentException.class, () -> CalculoFactura.calcular(carrito(), 0, 18, true, null));
    }

    @Test
    @DisplayName("Inválido: cantidad cero")
    void cantidadCero() {
        List<LineaVenta> lineas = new ArrayList<>();
        lineas.add(new LineaVenta("X", "Producto", 0, 10.00));
        assertThrows(IllegalArgumentException.class, () -> CalculoFactura.calcular(lineas, 0, 18, false, null));
    }
}
