package com.puntoventa;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import modelo.Empleado;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

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
    // Mas adelante esto se reemplaza por una consulta real.
    private Empleado empleadoDePrueba = new Empleado("1", "Admin", "Administrador", "admin", "1234");

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
