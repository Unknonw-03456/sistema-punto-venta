package modelo;

import java.time.LocalDateTime;

/** Fila del historial de ventas. */
public class VentaResumen {
    private final String numeroFactura;
    private final LocalDateTime fecha;
    private final String cliente;
    private final String cajero;
    private final String metodo;
    private final double total;
    private final String estado;

    public VentaResumen(String numeroFactura, LocalDateTime fecha, String cliente, String cajero,
                        String metodo, double total, String estado) {
        this.numeroFactura = numeroFactura;
        this.fecha = fecha;
        this.cliente = cliente;
        this.cajero = cajero;
        this.metodo = metodo;
        this.total = total;
        this.estado = estado;
    }

    public String getNumeroFactura() { return numeroFactura; }
    public String getCliente() { return cliente; }
    public String getCajero() { return cajero; }
    public String getMetodo() { return metodo == null ? "" : metodo; }
    public String getEstado() { return estado; }
    public String getFechaTexto() {
        return fecha == null ? "" : fecha.toString().replace('T', ' ').substring(0, 16);
    }
    public String getTotalTexto() { return String.format("$%,.2f", total); }
}
