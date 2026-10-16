package modelo;

/**
 * DTO solo para mostrar usuarios en la tabla del modulo de administracion.
 * No lleva contrasena ni hash: eso nunca sale de la base de datos hacia la vista.
 */
public class UsuarioAdmin {
    private int idUsuario;
    private String usuario;
    private String nombre;
    private String cargo;
    private String rolNombre;
    private String estado;

    public UsuarioAdmin() {
    }

    public UsuarioAdmin(int idUsuario, String usuario, String nombre, String cargo, String rolNombre, boolean activo) {
        this.idUsuario = idUsuario;
        this.usuario = usuario;
        this.nombre = nombre;
        this.cargo = cargo;
        this.rolNombre = rolNombre;
        this.estado = activo ? "Activo" : "Inactivo";
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
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

    public String getRolNombre() {
        return rolNombre;
    }

    public void setRolNombre(String rolNombre) {
        this.rolNombre = rolNombre;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
