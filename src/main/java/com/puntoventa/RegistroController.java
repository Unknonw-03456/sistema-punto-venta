package com.puntoventa;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegistroController {

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtApellido;

    @FXML
    private TextField txtUsuario;

    @FXML
    private TextField txtCorreo;

    @FXML
    private PasswordField txtContrasena;

    @FXML
    private PasswordField txtConfirmar;

    @FXML
    private ComboBox<String> cmbRol;

    @FXML
    private Button btnGuardar;

    @FXML
    private Label lblMensajeRegistro;

    // Se ejecuta automaticamente al cargar el FXML
    @FXML
    private void initialize() {
        cmbRol.getItems().addAll("Administrador", "Vendedor");
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText();
        String apellido = txtApellido.getText();
        String usuario = txtUsuario.getText();
        String correo = txtCorreo.getText();
        String contrasena = txtContrasena.getText();
        String confirmar = txtConfirmar.getText();
        String rol = cmbRol.getValue();

        if (nombre.isEmpty() || apellido.isEmpty() || usuario.isEmpty()
                || correo.isEmpty() || contrasena.isEmpty() || confirmar.isEmpty()) {
            lblMensajeRegistro.setText("Todos los campos son obligatorios.");
            return;
        }

        if (!correo.contains("@") || !correo.contains(".")) {
            lblMensajeRegistro.setText("Correo inválido.");
            return;
        }

        if (!contrasena.equals(confirmar)) {
            lblMensajeRegistro.setText("Las contraseñas no coinciden.");
            return;
        }

        if (rol == null) {
            lblMensajeRegistro.setText("Selecciona un rol.");
            return;
        }

        // Aqui mas adelante se creara un objeto Empleado/Usuario y se guardara.
        lblMensajeRegistro.setText("Registro exitoso para " + nombre + " " + apellido);
    }

    @FXML
    private void limpiar() {
        txtNombre.clear();
        txtApellido.clear();
        txtUsuario.clear();
        txtCorreo.clear();
        txtContrasena.clear();
        txtConfirmar.clear();
        cmbRol.setValue(null);
        lblMensajeRegistro.setText("");
    }

    @FXML
    private void cancelar() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }
}
