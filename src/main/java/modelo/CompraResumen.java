package modelo;

import java.time.LocalDateTime;

/** Fila del historial de compras. */
public class CompraResumen {
    private final int idCompra;
    private final LocalDateTime fecha;
    private final String proveedor;
    private final String numeroDocumento;
    private final double total;

    public CompraResumen(int idCompra, LocalDateTime fecha, String proveedor, String numeroDocumento, double total) {
        this.idCompra = idCompra;
        this.fecha = fecha;
        this.proveedor = proveedor;
        this.numeroDocumento = numeroDocumento;
        this.total = total;
    }

    public int getIdCompra() { return idCompra; }
    public String getProveedor() { return proveedor; }
    public String getNumeroDocumento() { return numeroDocumento == null ? "—" : numeroDocumento; }
    public double getTotal() { return total; }

    public String getFechaTexto() {
        return fecha == null ? "" : fecha.toString().replace('T', ' ').substring(0, 16);
    }

    public String getTotalTexto() {
        return String.format("$%,.2f", total);
    }
}
