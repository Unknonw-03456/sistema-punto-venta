package com.puntoventa;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.puntoventa.dao.TwoFactorDAO;
import com.puntoventa.util.TotpUtil;
import modelo.Empleado;

/**
 * Pantalla de configuración inicial de 2FA: genera un secreto nuevo, muestra el QR
 * para escanear con Google/Microsoft Authenticator, y pide un código de confirmación
 * antes de activarlo de verdad.
 */
public class Configurar2FAStage {

    public interface Callback {
        void onConfirmado();
        void onCancelado();
    }

    public static void mostrar(Stage ownerStage, Empleado empleado, Callback callback) {
        String secreto = TotpUtil.generarSecreto();
        String uri = TotpUtil.generarUriOtpAuth(empleado.getUsuario(), secreto);
        String urlQr = TotpUtil.generarUrlQr(uri);

        try {
            new TwoFactorDAO().guardarSecretoPendiente(empleado.getId(), secreto);
        } catch (Exception ex) {
            new Alert(AlertType.ERROR, "No se pudo preparar la verificación en dos pasos: " + ex.getMessage()).showAndWait();
            callback.onCancelado();
            return;
        }

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(ownerStage);
        modal.setTitle("Configurar verificación en dos pasos");

        Label lblInstrucciones = new Label(
                "Escanea este código con Google Authenticator o Microsoft Authenticator,\n"
                + "luego escribe el código de 6 dígitos que te muestre la app para confirmar.");
        lblInstrucciones.setWrapText(true);

        ImageView imagenQr = new ImageView(new Image(urlQr, true));
        imagenQr.setFitWidth(220);
        imagenQr.setFitHeight(220);

        TextField txtCodigo = new TextField();
        txtCodigo.setPromptText("Código de 6 dígitos");

        Label lblError = new Label();
        lblError.getStyleClass().add("alerta-error");
        lblError.setWrapText(true);

        Button btnConfirmar = new Button("Confirmar y activar");
        btnConfirmar.getStyleClass().add("button-primario");
        Button btnCancelar = new Button("Cancelar");

        btnConfirmar.setOnAction(e -> {
            if (!TotpUtil.validarCodigo(secreto, txtCodigo.getText().trim())) {
                lblError.setText("Código incorrecto. Verifica la hora de tu celular e intenta de nuevo.");
                return;
            }
            try {
                new TwoFactorDAO().activar(empleado.getId());
                modal.close();
                callback.onConfirmado();
            } catch (Exception ex) {
                lblError.setText("Error al activar: " + ex.getMessage());
            }
        });

        btnCancelar.setOnAction(e -> {
            modal.close();
            callback.onCancelado();
        });

        VBox contenedor = new VBox(14, lblInstrucciones, imagenQr, txtCodigo,
                new HBox(10, btnConfirmar, btnCancelar), lblError);
        contenedor.setPadding(new Insets(20));

        Scene scene = new Scene(contenedor);
        scene.getStylesheets().add(Configurar2FAStage.class.getResource("/com/puntoventa/css/styles.css").toExternalForm());
        modal.setScene(scene);
        modal.showAndWait();
    }
}
