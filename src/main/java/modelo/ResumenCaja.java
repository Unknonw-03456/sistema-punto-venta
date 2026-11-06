package modelo;

import java.util.LinkedHashMap;
import java.util.Map;

/** Resumen en vivo de una caja abierta: lo que debería haber en efectivo y ventas por método de pago. */
public class ResumenCaja {
    private final double montoInicial;
    private final double movimientosEfectivo;
    private final Map<String, Double> ventasPorMetodo = new LinkedHashMap<>();
    private final Map<String, Integer> cantidadPorMetodo = new LinkedHashMap<>();

    public ResumenCaja(double montoInicial, double movimientosEfectivo) {
        this.montoInicial = montoInicial;
        this.movimientosEfectivo = movimientosEfectivo;
    }

    public void agregarMetodo(String metodo, int cantidad, double total) {
        ventasPorMetodo.put(metodo, total);
        cantidadPorMetodo.put(metodo, cantidad);
    }

    public double getMontoInicial() { return montoInicial; }

    /** Entradas y salidas de efectivo (ventas en efectivo, anulaciones, ingresos y egresos). */
    public double getMovimientosEfectivo() { return movimientosEfectivo; }

    /** Efectivo que debería haber en la caja ahora: monto inicial + movimientos. */
    public double getMontoEsperado() {
        return Math.round((montoInicial + movimientosEfectivo) * 100.0) / 100.0;
    }

    public Map<String, Double> getVentasPorMetodo() { return ventasPorMetodo; }
    public Map<String, Integer> getCantidadPorMetodo() { return cantidadPorMetodo; }
}
