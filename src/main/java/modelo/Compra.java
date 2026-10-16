package modelo;

import java.time.LocalDate;

public class Compra {
    private int idCompra;
    private LocalDate fecha;
    private double total;
    private Proveedor proveedor;

    public Compra() {
    }

    public Compra(int idCompra, Proveedor proveedor, double total) {
        this.idCompra = idCompra;
        this.proveedor = proveedor;
        this.total = total;
        this.fecha = LocalDate.now();
    }

    public int getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(int idCompra) {
        this.idCompra = idCompra;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public String toString() {
        return "Compra #" + idCompra + " - " + proveedor + " - $" + total;
    }
}
