package com.puntoventa;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.puntoventa.dao.ClienteDAO;
import modelo.Cliente;

/**
 * Ventana modal para crear o editar un cliente.
 * Si clienteExistente es null, el modal crea uno nuevo; si no, lo edita.
 * alGuardar se ejecuta despues de guardar con exito, para refrescar la tabla de la ventana principal.
 */
public class ClienteModal {

    public static void mostrar(Stage ownerStage, Cliente clienteExistente, Runnable alGuardar) {
        boolean esEdicion = clienteExistente != null;
        ClienteDAO clienteDAO = new ClienteDAO();

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(ownerStage);
        modal.setTitle(esEdicion ? "Editar cliente" : "Nuevo cliente");

        TextField txtCedula = new TextField();
        txtCedula.setPromptText("Cedula");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre");
        TextField txtTelefono = new TextField();
        txtTelefono.setPromptText("Telefono");
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email");

        if (esEdicion) {
            txtCedula.setText(clienteExistente.getCedula());
            txtCedula.setDisable(true); // la clave primaria no se cambia al editar
            txtNombre.setText(clienteExistente.getNombre());
            txtTelefono.setText(clienteExistente.getTelefono());
            txtEmail.setText(clienteExistente.getEmail());
        }

        Label lblError = new Label();
        lblError.getStyleClass().add("alerta-error");
        lblError.setWrapText(true);
        lblError.setMaxWidth(260);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(10);
        form.setPadding(new Insets(16));
        form.addRow(0, new Label("Cedula:"), txtCedula);
        form.addRow(1, new Label("Nombre:"), txtNombre);
        form.addRow(2, new Label("Telefono:"), txtTelefono);
        form.addRow(3, new Label("Email:"), txtEmail);

        Button btnGuardar = new Button("Guardar");
        btnGuardar.getStyleClass().add("button-primario");
        Button btnCancelar = new Button("Cancelar");

        btnGuardar.setOnAction(e -> {
            lblError.setText("");

            if (txtCedula.getText().isBlank() || txtNombre.getText().isBlank()
                    || txtTelefono.getText().isBlank() || txtEmail.getText().isBlank()) {
                lblError.setText("Todos los campos son obligatorios.");
                return;
            }

            if (!txtEmail.getText().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                lblError.setText("El email no tiene un formato valido.");
                return;
            }

            try {
                if (esEdicion) {
                    clienteDAO.actualizar(txtCedula.getText(), txtNombre.getText(), txtTelefono.getText(), txtEmail.getText());
                } else {
                    clienteDAO.crear(txtCedula.getText(), txtNombre.getText(), txtTelefono.getText(), txtEmail.getText());
                }

                modal.close();
                alGuardar.run();
            } catch (Exception ex) {
                lblError.setText("Error al guardar: " + ex.getMessage());
            }
        });

        btnCancelar.setOnAction(e -> modal.close());

        HBox botones = new HBox(10, btnGuardar, btnCancelar);
        VBox contenedor = new VBox(10, form, botones, lblError);
        contenedor.setPadding(new Insets(10));

        Scene scene = new Scene(contenedor);
        scene.getStylesheets().add(ClienteModal.class.getResource("/com/puntoventa/css/styles.css").toExternalForm());
        modal.setScene(scene);
        modal.showAndWait();
    }
}
