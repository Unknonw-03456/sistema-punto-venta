package com.puntoventa;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.puntoventa.dao.BitacoraDAO;
import com.puntoventa.dao.TwoFactorDAO;
import com.puntoventa.util.TotpUtil;
import modelo.Empleado;

/**
 * Pantalla de verificación en dos pasos del login. Máximo 3 intentos; al tercer
 * fallo bloquea la cuenta 5 minutos y registra cada intento en la bitácora de accesos.
 */
public class Verificar2FAStage {

    public interface Callback {
        void onVerificado();
        void onCancelado();
    }

    public static void mostrar(Stage ownerStage, Empleado empleado, String secreto, Callback callback) {
        TwoFactorDAO twoFactorDAO = new TwoFactorDAO();
        BitacoraDAO bitacoraDAO = new BitacoraDAO();

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(ownerStage);
        modal.setTitle("Verificación en dos pasos");

        Label lblInstrucciones = new Label("Escribe el código de 6 dígitos de tu app Authenticator.");
        TextField txtCodigo = new TextField();
        txtCodigo.setPromptText("Código de 6 dígitos");

        Label lblError = new Label();
        lblError.getStyleClass().add("alerta-error");
        lblError.setWrapText(true);

        Button btnVerificar = new Button("Verificar");
        btnVerificar.getStyleClass().add("button-primario");
        Button btnCancelar = new Button("Cancelar");

        btnVerificar.setOnAction(e -> {
            lblError.setText("");

            LocalDateTime bloqueoHasta = twoFactorDAO.obtenerBloqueoHasta(empleado.getId());
            if (bloqueoHasta != null && bloqueoHasta.isAfter(LocalDateTime.now())) {
                String hora = bloqueoHasta.format(DateTimeFormatter.ofPattern("HH:mm"));
                lblError.setText("Cuenta bloqueada temporalmente por intentos fallidos. Intenta después de las " + hora + ".");
                return;
            }

            if (TotpUtil.validarCodigo(secreto, txtCodigo.getText().trim())) {
                try {
                    twoFactorDAO.resetearIntentos(empleado.getId());
                } catch (Exception ex) {
                    System.err.println("No se pudo reiniciar intentos: " + ex.getMessage());
                }
                bitacoraDAO.registrar(empleado.getUsuario(), empleado.getId(), true, "Login con 2FA correcto");
                modal.close();
                callback.onVerificado();
                return;
            }

            try {
                twoFactorDAO.registrarIntentoFallido(empleado.getId());
            } catch (Exception ex) {
                System.err.println("No se pudo registrar el intento fallido: " + ex.getMessage());
            }
            bitacoraDAO.registrar(empleado.getUsuario(), empleado.getId(), false, "Código 2FA incorrecto");

            LocalDateTime bloqueoAhora = twoFactorDAO.obtenerBloqueoHasta(empleado.getId());
            if (bloqueoAhora != null && bloqueoAhora.isAfter(LocalDateTime.now())) {
                lblError.setText("Código incorrecto. Se alcanzó el máximo de intentos: cuenta bloqueada 5 minutos.");
            } else {
                lblError.setText("Código incorrecto. Intenta de nuevo.");
            }
            txtCodigo.clear();
        });

        btnCancelar.setOnAction(e -> {
            modal.close();
            callback.onCancelado();
        });

        VBox contenedor = new VBox(14, lblInstrucciones, txtCodigo,
                new HBox(10, btnVerificar, btnCancelar), lblError);
        contenedor.setPadding(new Insets(20));

        Scene scene = new Scene(contenedor);
        scene.getStylesheets().add(Verificar2FAStage.class.getResource("/com/puntoventa/css/styles.css").toExternalForm());
        modal.setScene(scene);
        modal.showAndWait();
    }
}
