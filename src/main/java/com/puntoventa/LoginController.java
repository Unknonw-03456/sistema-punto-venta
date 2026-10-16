package com.puntoventa;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import com.puntoventa.dao.BitacoraDAO;
import com.puntoventa.dao.TwoFactorDAO;
import com.puntoventa.dao.UsuarioDAO;
import modelo.Empleado;

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

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TwoFactorDAO twoFactorDAO = new TwoFactorDAO();
    private final BitacoraDAO bitacoraDAO = new BitacoraDAO();

    @FXML
    private void iniciarSesion() {
        String usuario = txtUsuario.getText();
        String contrasena = txtContrasena.getText();

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            lblMensaje.setText("Debes llenar usuario y contraseña.");
            return;
        }

        Empleado empleado = usuarioDAO.autenticar(usuario, contrasena);
        if (empleado == null) {
            bitacoraDAO.registrar(usuario, null, false, "Usuario o contraseña incorrectos");
            lblMensaje.setText("Usuario o contraseña incorrectos, o la cuenta está desactivada.");
            return;
        }

        LocalDateTime bloqueoHasta = twoFactorDAO.obtenerBloqueoHasta(empleado.getId());
        if (bloqueoHasta != null && bloqueoHasta.isAfter(LocalDateTime.now())) {
            String hora = bloqueoHasta.format(DateTimeFormatter.ofPattern("HH:mm"));
            bitacoraDAO.registrar(usuario, empleado.getId(), false, "Intento con cuenta bloqueada");
            lblMensaje.setText("Cuenta bloqueada temporalmente. Intenta después de las " + hora + ".");
            return;
        }

        Stage stage = (Stage) btnIniciar.getScene().getWindow();
        TwoFactorDAO.Estado2FA estado = twoFactorDAO.obtenerEstado(empleado.getId());

        if (estado == null || !estado.activado) {
            Configurar2FAStage.mostrar(stage, empleado, new Configurar2FAStage.Callback() {
                @Override
                public void onConfirmado() {
                    bitacoraDAO.registrar(usuario, empleado.getId(), true, "2FA configurado y login correcto");
                    Sesion.iniciar(empleado);
                    abrirDashboard();
                }

                @Override
                public void onCancelado() {
                    lblMensaje.setText("Configuración de 2FA cancelada. Vuelve a intentar para configurarla.");
                }
            });
        } else {
            Verificar2FAStage.mostrar(stage, empleado, estado.secreto, new Verificar2FAStage.Callback() {
                @Override
                public void onVerificado() {
                    Sesion.iniciar(empleado);
                    abrirDashboard();
                }

                @Override
                public void onCancelado() {
                    lblMensaje.setText("Verificación en dos pasos cancelada.");
                }
            });
        }
    }

    private void abrirDashboard() {
        DashboardController dashboard = new DashboardController();
        Stage stage = (Stage) btnIniciar.getScene().getWindow();
        stage.setScene(dashboard.crearEscena());
        stage.setTitle("Panel - Sistema Punto de Venta ("
                + Sesion.getActual().getNombre() + " - " + Sesion.getRolActual() + ")");
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
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/puntoventa/css/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
}
