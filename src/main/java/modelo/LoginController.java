package modelo;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import modelo.Empleado;
import modelo.Rol;

public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtContrasena;

    @FXML
    private Button btnIniciar;

    @FXML
    private Button btnSalir;

    @FXML
    private Label lblMensaje;

    // Empleado de prueba mientras no hay base de datos.
    // Mas adelante esto se reemplaza por una consulta real (Tarea 3).
    private Rol rolAdmin = new Rol(1, "Administrador");
    private Empleado empleadoDePrueba = new Empleado(1, "admin", "1234", "Admin", "Administrador", rolAdmin);

    @FXML
    private void iniciarSesion() {
        String usuario = txtUsuario.getText();
        String contrasena = txtContrasena.getText();

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            lblMensaje.setText("Debes llenar usuario y contraseña.");
            return;
        }

        if (empleadoDePrueba.validarCredenciales(usuario, contrasena)) {
            lblMensaje.setText("Acceso correcto. Bienvenido " + empleadoDePrueba.getNombre());
        } else {
            lblMensaje.setText("Usuario o contraseña incorrectos.");
        }

        System.out.println("Usuario escrito: [" + usuario + "]"); 

        System.out.println("Contrasena escrita: [" + contrasena + "]");
    }

    @FXML
    private void salir() {
        Stage stage = (Stage) btnSalir.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void abrirRegistro() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("registro.fxml"));
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setTitle("Registro de Usuario");
        stage.setScene(new Scene(root));
        stage.show();
    }
}
