package com.puntoventa.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import modelo.LineaVenta;

/**
 * Cálculo de los totales de una factura. Es lógica pura (sin base de datos ni pantalla),
 * así se prueba con JUnit y se usa igual en la pantalla (totales en vivo) y en el guardado.
 *
 * Reglas: los precios NO incluyen ITBIS. El descuento (porcentaje) se aplica sobre el
 * subtotal, y el ITBIS se calcula sobre el valor ya descontado.
 */
public final class CalculoFactura {

    private CalculoFactura() {
    }

    public static final class Resultado {
        public final BigDecimal subtotal;
        public final BigDecimal descuento;
        public final BigDecimal baseImponible;
        public final BigDecimal itbis;
        public final BigDecimal total;
        public final BigDecimal cambio;

        Resultado(BigDecimal subtotal, BigDecimal descuento, BigDecimal baseImponible,
                  BigDecimal itbis, BigDecimal total, BigDecimal cambio) {
            this.subtotal = subtotal;
            this.descuento = descuento;
            this.baseImponible = baseImponible;
            this.itbis = itbis;
            this.total = total;
            this.cambio = cambio;
        }
    }

    /**
     * @param lineas              productos del carrito (al menos uno)
     * @param descuentoPorcentaje 0 a 100
     * @param itbisPorcentaje     0 o más (por defecto 18)
     * @param pagoEnEfectivo      true si se paga en efectivo (entonces se valida el efectivo recibido)
     * @param efectivoRecibido    monto entregado por el cliente (solo importa si es efectivo)
     * @throws IllegalArgumentException con un mensaje claro si algún dato es inválido
     */
    public static Resultado calcular(List<LineaVenta> lineas, double descuentoPorcentaje,
                                     double itbisPorcentaje, boolean pagoEnEfectivo,
                                     Double efectivoRecibido) {
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío.");
        }
        if (Double.isNaN(descuentoPorcentaje) || descuentoPorcentaje < 0 || descuentoPorcentaje > 100) {
            throw new IllegalArgumentException("El descuento debe estar entre 0 y 100%.");
        }
        if (Double.isNaN(itbisPorcentaje) || itbisPorcentaje < 0) {
            throw new IllegalArgumentException("El porcentaje de ITBIS no puede ser negativo.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineaVenta linea : lineas) {
            if (linea.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad de \"" + linea.getNombre() + "\" debe ser mayor a 0.");
            }
            if (linea.getPrecioUnitario() < 0) {
                throw new IllegalArgumentException("El precio de \"" + linea.getNombre() + "\" no puede ser negativo.");
            }
            BigDecimal importe = BigDecimal.valueOf(linea.getPrecioUnitario())
                    .multiply(BigDecimal.valueOf(linea.getCantidad()))
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(importe);
        }

        BigDecimal descuento = subtotal.multiply(BigDecimal.valueOf(descuentoPorcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal baseImponible = subtotal.subtract(descuento);
        BigDecimal itbis = baseImponible.multiply(BigDecimal.valueOf(itbisPorcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = baseImponible.add(itbis);

        BigDecimal cambio = BigDecimal.ZERO.setScale(2);
        if (pagoEnEfectivo) {
            if (efectivoRecibido == null || efectivoRecibido.isNaN()) {
                throw new IllegalArgumentException("Indica el efectivo recibido.");
            }
            BigDecimal recibido = BigDecimal.valueOf(efectivoRecibido).setScale(2, RoundingMode.HALF_UP);
            if (recibido.compareTo(total) < 0) {
                throw new IllegalArgumentException("El efectivo recibido es menor que el total a pagar.");
            }
            cambio = recibido.subtract(total);
        }

        return new Resultado(subtotal, descuento, baseImponible, itbis, total, cambio);
    }
}
