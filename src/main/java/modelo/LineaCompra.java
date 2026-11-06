package modelo;

/** Una línea de una compra a proveedor: producto, cantidad que entra y costo unitario. */
public class LineaCompra {
    private final String codigo;
    private final String nombre;
    private int cantidad;
    private final double costoUnitario;

    public LineaCompra(String codigo, String nombre, int cantidad, double costoUnitario) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getCostoUnitario() { return costoUnitario; }

    public double getSubtotal() {
        return Math.round(cantidad * costoUnitario * 100.0) / 100.0;
    }
}
