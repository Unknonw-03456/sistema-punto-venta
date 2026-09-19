package modelo;

public class Empleado {
    private String id;
    private String nombre;
    private String cargo;
    private String usuario;
    private String contrasena;

    public Empleado(String id, String nombre, String cargo, String usuario, String contrasena) {
        this.id = id;
        this.nombre = nombre;
        this.cargo = cargo;
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    // Usado por la pantalla de Login (Mini Practica No. 1)
    public boolean validarCredenciales(String usuarioIngresado, String contrasenaIngresada) {
        return this.usuario.equals(usuarioIngresado) && this.contrasena.equals(contrasenaIngresada);
    }

    @Override
    public String toString() {
        return nombre + " - " + cargo;
    }
}
