package modelo;

public class Empleado extends Usuario {
    private String nombre;
    private String cargo;
    private Rol rol;

    public Empleado() {
        super();
    }

    public Empleado(int id, String usuario, String contrasena, String nombre, String cargo, Rol rol) {
        super(id, usuario, contrasena);
        this.nombre = nombre;
        this.cargo = cargo;
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return nombre + " - " + cargo;
    }
}
