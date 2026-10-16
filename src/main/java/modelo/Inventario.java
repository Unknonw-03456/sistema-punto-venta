package modelo;

import java.util.ArrayList;

public class Inventario {
    private int idInventario;
    private ArrayList<Producto> productos;

    public Inventario() {
        this.productos = new ArrayList<>();
    }

    public Inventario(int idInventario) {
        this.idInventario = idInventario;
        this.productos = new ArrayList<>();
    }

    public int getIdInventario() {
        return idInventario;
    }

    public void setIdInventario(int idInventario) {
        this.idInventario = idInventario;
    }

    public ArrayList<Producto> getProductos() {
        return productos;
    }

    public void agregarProducto(Producto producto) {
        productos.add(producto);
    }

    public void actualizarStock(Producto producto, int cantidad) {
        producto.reducirStock(cantidad);
    }

    @Override
    public String toString() {
        return "Inventario #" + idInventario + " (" + productos.size() + " productos)";
    }
}
