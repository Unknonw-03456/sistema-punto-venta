package com.puntoventa;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.puntoventa.dao.CategoriaDAO;
import com.puntoventa.dao.ProductoDAO;
import com.puntoventa.dao.ProveedorDAO;
import modelo.Categoria;
import modelo.Producto;
import modelo.Proveedor;

/**
 * Ventana modal para crear o editar un producto.
 * Categoría y proveedor se eligen de listas (ComboBox) que salen de la BD, con un
 * botón "+" para crear uno nuevo al momento: así nunca se escribe un id que no existe
 * (evita el error de llave foránea).
 */
public class ProductoModal {

    public static void mostrar(Stage ownerStage, Producto productoExistente, Runnable alGuardar) {
        boolean esEdicion = productoExistente != null;
        ProductoDAO productoDAO = new ProductoDAO();
        CategoriaDAO categoriaDAO = new CategoriaDAO();
        ProveedorDAO proveedorDAO = new ProveedorDAO();

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(ownerStage);
        modal.setTitle(esEdicion ? "Editar producto" : "Nuevo producto");

        TextField txtCodigo = new TextField();
        txtCodigo.setPromptText("Código (ej. P003)");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre");
        TextField txtPrecio = new TextField();
        txtPrecio.setPromptText("Precio (ej. 12500.50)");
        TextField txtStock = new TextField();
        txtStock.setPromptText("Stock");

        ComboBox<Categoria> comboCategoria = new ComboBox<>(FXCollections.observableArrayList(categoriaDAO.listar()));
        comboCategoria.setPromptText("Selecciona una categoría");
        comboCategoria.setPrefWidth(220);
        Button btnNuevaCategoria = new Button("+");

        ComboBox<Proveedor> comboProveedor = new ComboBox<>(FXCollections.observableArrayList(proveedorDAO.listar()));
        comboProveedor.setPromptText("Selecciona un proveedor");
        comboProveedor.setPrefWidth(220);
        Button btnNuevoProveedor = new Button("+");

        if (esEdicion) {
            txtCodigo.setText(productoExistente.getCodigo());
            txtCodigo.setDisable(true); // la clave primaria no se cambia al editar
            txtNombre.setText(productoExistente.getNombre());
            txtPrecio.setText(String.valueOf(productoExistente.getPrecio()));
            txtStock.setText(String.valueOf(productoExistente.getStock()));
            if (productoExistente.getCategoria() != null) {
                int idCat = productoExistente.getCategoria().getIdCategoria();
                comboCategoria.getItems().stream().filter(c -> c.getIdCategoria() == idCat)
                        .findFirst().ifPresent(comboCategoria::setValue);
            }
            if (productoExistente.getProveedor() != null) {
                int idProv = productoExistente.getProveedor().getIdProveedor();
                comboProveedor.getItems().stream().filter(p -> p.getIdProveedor() == idProv)
                        .findFirst().ifPresent(comboProveedor::setValue);
            }
        }

        Label lblError = new Label();
        lblError.getStyleClass().add("alerta-error");
        lblError.setWrapText(true);
        lblError.setMaxWidth(340);

        // "+" crea una categoría nueva y la deja seleccionada
        btnNuevaCategoria.setOnAction(e -> {
            TextInputDialog dialogo = new TextInputDialog();
            dialogo.setTitle("Nueva categoría");
            dialogo.setHeaderText(null);
            dialogo.setContentText("Nombre de la categoría:");
            dialogo.showAndWait().map(String::trim).filter(n -> !n.isEmpty()).ifPresent(nombre -> {
                try {
                    categoriaDAO.crear(nombre, "");
                    comboCategoria.setItems(FXCollections.observableArrayList(categoriaDAO.listar()));
                    comboCategoria.getItems().stream().filter(c -> c.getNombre().equals(nombre))
                            .reduce((primero, ultimo) -> ultimo).ifPresent(comboCategoria::setValue);
                } catch (Exception ex) {
                    lblError.setText("No se pudo crear la categoría: " + ex.getMessage());
                }
            });
        });

        // "+" crea un proveedor nuevo y lo deja seleccionado
        btnNuevoProveedor.setOnAction(e -> {
            TextInputDialog dialogo = new TextInputDialog();
            dialogo.setTitle("Nuevo proveedor");
            dialogo.setHeaderText(null);
            dialogo.setContentText("Nombre del proveedor:");
            dialogo.showAndWait().map(String::trim).filter(n -> !n.isEmpty()).ifPresent(nombre -> {
                try {
                    proveedorDAO.crear(nombre, "");
                    comboProveedor.setItems(FXCollections.observableArrayList(proveedorDAO.listar()));
                    comboProveedor.getItems().stream().filter(p -> p.getNombre().equals(nombre))
                            .reduce((primero, ultimo) -> ultimo).ifPresent(comboProveedor::setValue);
                } catch (Exception ex) {
                    lblError.setText("No se pudo crear el proveedor: " + ex.getMessage());
                }
            });
        });

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(10);
        form.setPadding(new Insets(16));
        form.addRow(0, new Label("Código:"), txtCodigo);
        form.addRow(1, new Label("Nombre:"), txtNombre);
        form.addRow(2, new Label("Precio:"), txtPrecio);
        form.addRow(3, new Label("Stock:"), txtStock);
        form.addRow(4, new Label("Categoría:"), new HBox(6, comboCategoria, btnNuevaCategoria));
        form.addRow(5, new Label("Proveedor:"), new HBox(6, comboProveedor, btnNuevoProveedor));

        Button btnGuardar = new Button("Guardar");
        btnGuardar.getStyleClass().add("button-primario");
        Button btnCancelar = new Button("Cancelar");

        btnGuardar.setOnAction(e -> {
            lblError.setText("");

            if (txtCodigo.getText().isBlank() || txtNombre.getText().isBlank()
                    || txtPrecio.getText().isBlank() || txtStock.getText().isBlank()
                    || comboCategoria.getValue() == null || comboProveedor.getValue() == null) {
                lblError.setText("Todos los campos son obligatorios (incluida la categoría y el proveedor).");
                return;
            }

            double precio;
            int stock;
            try {
                // Se quitan comas y espacios de miles: "12,000" se interpreta como 12000.
                precio = Double.parseDouble(txtPrecio.getText().replace(",", "").replace(" ", ""));
                stock = Integer.parseInt(txtStock.getText().trim());
            } catch (NumberFormatException ex) {
                lblError.setText("Precio y stock deben ser números.");
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

            int idCategoria = comboCategoria.getValue().getIdCategoria();
            int idProveedor = comboProveedor.getValue().getIdProveedor();

            try {
                if (esEdicion) {
                    productoDAO.actualizar(txtCodigo.getText().trim(), txtNombre.getText().trim(), precio, stock, idCategoria, idProveedor);
                } else {
                    productoDAO.crear(txtCodigo.getText().trim(), txtNombre.getText().trim(), precio, stock, idCategoria, idProveedor);
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
