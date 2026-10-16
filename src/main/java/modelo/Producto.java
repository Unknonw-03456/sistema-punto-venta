package modelo;

public class Producto {
    private String codigo;
    private String nombre;
    private double precio;
    private int stock;
    private Categoria categoria;
    private Proveedor proveedor;

    public Producto() {
    }

    public Producto(String codigo, String nombre, double precio, int stock,
                     Categoria categoria, Proveedor proveedor) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.categoria = categoria;
        this.proveedor = proveedor;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    // Reduce el stock cuando se vende el producto
    public void reducirStock(int cantidad) {
        if (cantidad <= this.stock) {
            this.stock -= cantidad;
        } else {
            System.out.println("Stock insuficiente para " + nombre);
        }
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre + " ($" + precio + ") Stock: " + stock;
    }
}
