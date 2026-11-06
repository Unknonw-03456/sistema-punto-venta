package modelo;

/** Una línea del carrito de ventas: producto, cantidad y precio unitario. */
public class LineaVenta {
    private final String codigo;
    private final String nombre;
    private int cantidad;
    private final double precioUnitario;

    public LineaVenta(String codigo, String nombre, int cantidad, double precioUnitario) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    /** Subtotal de la línea (cantidad x precio), redondeado a 2 decimales. */
    public double getSubtotal() {
        return Math.round(cantidad * precioUnitario * 100.0) / 100.0;
    }
}
