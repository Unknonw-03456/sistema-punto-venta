package modelo;

import java.time.LocalDate;
import java.util.ArrayList;

public class Venta {
    private String numeroFactura;
    private LocalDate fecha;
    private Cliente cliente;
    private Empleado empleado;
    private ArrayList<DetalleVenta> detalles;

    public Venta(String numeroFactura, Cliente cliente, Empleado empleado) {
        this.numeroFactura = numeroFactura;
        this.fecha = LocalDate.now();
        this.cliente = cliente;
        this.empleado = empleado;
        this.detalles = new ArrayList<>();
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public ArrayList<DetalleVenta> getDetalles() {
        return detalles;
    }

    // Agrega un producto a la venta y reduce su stock
    public void agregarDetalle(Producto producto, int cantidad) {
        DetalleVenta detalle = new DetalleVenta(producto, cantidad);
        detalles.add(detalle);
        producto.reducirStock(cantidad);
    }

    public double calcularTotal() {
        double total = 0;
        for (DetalleVenta d : detalles) {
            total += d.calcularSubtotal();
        }
        return total;
    }

    @Override
    public String toString() {
        return "Factura " + numeroFactura + " - " + cliente.getNombre() + " - Total: $" + calcularTotal();
    }
}
