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

import com.puntoventa.dao.ProductoDAO;
import modelo.Producto;

/**
 * Ventana modal para crear o editar un producto.
 * Si productoExistente es null, el modal crea uno nuevo; si no, lo edita.
 * alGuardar se ejecuta despues de guardar con exito, para refrescar la tabla de la ventana principal.
 */
public class ProductoModal {

    public static void mostrar(Stage ownerStage, Producto productoExistente, Runnable alGuardar) {
        boolean esEdicion = productoExistente != null;
        ProductoDAO productoDAO = new ProductoDAO();

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(ownerStage);
        modal.setTitle(esEdicion ? "Editar producto" : "Nuevo producto");

        TextField txtCodigo = new TextField();
        txtCodigo.setPromptText("Codigo (ej. P003)");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre");
        TextField txtPrecio = new TextField();
        txtPrecio.setPromptText("Precio");
        TextField txtStock = new TextField();
        txtStock.setPromptText("Stock");
        TextField txtIdCategoria = new TextField();
        txtIdCategoria.setPromptText("ID Categoria");
        TextField txtIdProveedor = new TextField();
        txtIdProveedor.setPromptText("ID Proveedor");

        if (esEdicion) {
            txtCodigo.setText(productoExistente.getCodigo());
            txtCodigo.setDisable(true); // la clave primaria no se cambia al editar
            txtNombre.setText(productoExistente.getNombre());
            txtPrecio.setText(String.valueOf(productoExistente.getPrecio()));
            txtStock.setText(String.valueOf(productoExistente.getStock()));
        }

        Label lblError = new Label();
        lblError.getStyleClass().add("alerta-error");
        lblError.setWrapText(true);
        lblError.setMaxWidth(260);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(10);
        form.setPadding(new Insets(16));
        form.addRow(0, new Label("Codigo:"), txtCodigo);
        form.addRow(1, new Label("Nombre:"), txtNombre);
        form.addRow(2, new Label("Precio:"), txtPrecio);
        form.addRow(3, new Label("Stock:"), txtStock);
        form.addRow(4, new Label("ID Categoria:"), txtIdCategoria);
        form.addRow(5, new Label("ID Proveedor:"), txtIdProveedor);

        Button btnGuardar = new Button("Guardar");
        btnGuardar.getStyleClass().add("button-primario");
        Button btnCancelar = new Button("Cancelar");

        btnGuardar.setOnAction(e -> {
            lblError.setText("");

            if (txtCodigo.getText().isBlank() || txtNombre.getText().isBlank()
                    || txtPrecio.getText().isBlank() || txtStock.getText().isBlank()
                    || txtIdCategoria.getText().isBlank() || txtIdProveedor.getText().isBlank()) {
                lblError.setText("Todos los campos son obligatorios.");
                return;
            }

            double precio;
            int stock;
            int idCategoria;
            int idProveedor;
            try {
                precio = Double.parseDouble(txtPrecio.getText());
                stock = Integer.parseInt(txtStock.getText());
                idCategoria = Integer.parseInt(txtIdCategoria.getText());
                idProveedor = Integer.parseInt(txtIdProveedor.getText());
            } catch (NumberFormatException ex) {
                lblError.setText("Precio, stock, categoria y proveedor deben ser numeros.");
                return;
            }

            if (precio <= 0) {
                lblError.setText("El precio debe ser mayor a 0.");
                return;
            }
            if (stock < 0) {
                lblError.setText("El stock no puede ser negativo.");
                return;
            }

            try {
                if (esEdicion) {
                    productoDAO.actualizar(txtCodigo.getText(), txtNombre.getText(), precio, stock, idCategoria, idProveedor);
                } else {
                    productoDAO.crear(txtCodigo.getText(), txtNombre.getText(), precio, stock, idCategoria, idProveedor);
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
        scene.getStylesheets().add(ProductoModal.class.getResource("/com/puntoventa/css/styles.css").toExternalForm());
        modal.setScene(scene);
        modal.showAndWait();
    }
}
