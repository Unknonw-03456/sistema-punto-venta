package com.puntoventa;

import com.puntoventa.dao.CompraDAO;
import com.puntoventa.dao.ProductoDAO;
import com.puntoventa.dao.ProveedorDAO;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import modelo.CompraResumen;
import modelo.LineaCompra;
import modelo.Producto;
import modelo.Proveedor;

/** Pantalla de Compras: registra entradas de mercancía (sube stock y actualiza costo). */
public class CompraPantalla {

    public static Node crear() {
        CompraDAO compraDAO = new CompraDAO();

        // ---------- Izquierda: catálogo de productos ----------
        ObservableList<Producto> productos = new ProductoDAO().listar();
        FilteredList<Producto> filtrados = new FilteredList<>(productos, p -> true);

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar producto por nombre o código...");
        txtBuscar.textProperty().addListener((o, a, texto) -> {
            String t = texto == null ? "" : texto.trim().toLowerCase();
            filtrados.setPredicate(p -> t.isEmpty()
                    || p.getNombre().toLowerCase().contains(t)
                    || p.getCodigo().toLowerCase().contains(t));
        });

        TableView<Producto> tablaCatalogo = new TableView<>(filtrados);
        TableColumn<Producto, String> cCod = new TableColumn<>("Código");
        cCod.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        TableColumn<Producto, String> cNom = new TableColumn<>("Producto");
        cNom.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        cNom.setPrefWidth(200);
        TableColumn<Producto, Integer> cStock = new TableColumn<>("Stock");
        cStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        tablaCatalogo.getColumns().addAll(cCod, cNom, cStock);
        tablaCatalogo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaCatalogo.setPlaceholder(new Label("No hay productos"));
        VBox.setVgrow(tablaCatalogo, Priority.ALWAYS);

        TextField txtCantidad = new TextField("1");
        txtCantidad.setPrefWidth(80);
        TextField txtCosto = new TextField();
        txtCosto.setPromptText("Costo unitario");
        txtCosto.setPrefWidth(120);
        Button btnAgregar = new Button("Agregar a la compra");
        btnAgregar.getStyleClass().add("button-acento");

        HBox filaAgregar = new HBox(8, new Label("Cant:"), txtCantidad, new Label("Costo:"), txtCosto, btnAgregar);
        filaAgregar.setAlignment(Pos.CENTER_LEFT);

        Label lblCatalogo = new Label("Productos");
        lblCatalogo.getStyleClass().add("label-titulo");
        VBox izquierda = new VBox(8, lblCatalogo, txtBuscar, tablaCatalogo, filaAgregar);
        izquierda.setPadding(new Insets(12));

        // ---------- Derecha: datos de la compra + carrito ----------
        ComboBox<Proveedor> cmbProveedor = new ComboBox<>(new ProveedorDAO().listar());
        cmbProveedor.setPromptText("Seleccione proveedor");
        cmbProveedor.setMaxWidth(Double.MAX_VALUE);
        TextField txtDocumento = new TextField();
        txtDocumento.setPromptText("N.º de factura del proveedor (opcional)");

        ObservableList<LineaCompra> carrito = FXCollections.observableArrayList();
        TableView<LineaCompra> tablaCarrito = new TableView<>(carrito);
        TableColumn<LineaCompra, String> kNom = new TableColumn<>("Producto");
        kNom.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        kNom.setPrefWidth(170);
        TableColumn<LineaCompra, Integer> kCant = new TableColumn<>("Cant.");
        kCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        TableColumn<LineaCompra, Double> kCosto = new TableColumn<>("Costo");
        kCosto.setCellValueFactory(new PropertyValueFactory<>("costoUnitario"));
        TableColumn<LineaCompra, Double> kSub = new TableColumn<>("Subtotal");
        kSub.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getSubtotal()).asObject());
        tablaCarrito.getColumns().addAll(kNom, kCant, kCosto, kSub);
        tablaCarrito.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaCarrito.setPlaceholder(new Label("Agregue productos a la compra"));
        VBox.setVgrow(tablaCarrito, Priority.ALWAYS);

        Label lblTotal = new Label("Total: $0.00");
        lblTotal.getStyleClass().add("label-titulo");
        Runnable recalcular = () -> {
            double t = 0;
            for (LineaCompra l : carrito) t += l.getSubtotal();
            lblTotal.setText(String.format("Total: $%,.2f", t));
        };

        Button btnQuitar = new Button("Quitar línea");
        btnQuitar.setOnAction(e -> {
            LineaCompra sel = tablaCarrito.getSelectionModel().getSelectedItem();
            if (sel != null) { carrito.remove(sel); recalcular.run(); }
        });
        Button btnVaciar = new Button("Vaciar");
        btnVaciar.setOnAction(e -> { carrito.clear(); recalcular.run(); });
        Button btnRegistrar = new Button("Registrar compra");
        btnRegistrar.getStyleClass().add("button-primario");

        HBox acciones = new HBox(8, btnQuitar, btnVaciar, btnRegistrar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        Label lblCompra = new Label("Compra a proveedor");
        lblCompra.getStyleClass().add("label-titulo");
        VBox derecha = new VBox(8, lblCompra, cmbProveedor, txtDocumento, tablaCarrito, lblTotal, acciones);
        derecha.setPadding(new Insets(12));

        // ---------- Historial ----------
        TableView<CompraResumen> tablaHist = new TableView<>();
        TableColumn<CompraResumen, Integer> hId = new TableColumn<>("N.º");
        hId.setCellValueFactory(new PropertyValueFactory<>("idCompra"));
        TableColumn<CompraResumen, String> hFecha = new TableColumn<>("Fecha");
        hFecha.setCellValueFactory(new PropertyValueFactory<>("fechaTexto"));
        TableColumn<CompraResumen, String> hProv = new TableColumn<>("Proveedor");
        hProv.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        TableColumn<CompraResumen, String> hDoc = new TableColumn<>("Documento");
        hDoc.setCellValueFactory(new PropertyValueFactory<>("numeroDocumento"));
        TableColumn<CompraResumen, String> hTot = new TableColumn<>("Total");
        hTot.setCellValueFactory(new PropertyValueFactory<>("totalTexto"));
        tablaHist.getColumns().addAll(hId, hFecha, hProv, hDoc, hTot);
        tablaHist.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaHist.setPlaceholder(new Label("Aún no hay compras registradas"));
        tablaHist.setPrefHeight(170);
        tablaHist.setItems(FXCollections.observableArrayList(compraDAO.historial()));
        Label lblHist = new Label("Historial de compras (últimas 50)");
        lblHist.getStyleClass().add("label-titulo");
        VBox historial = new VBox(6, lblHist, tablaHist);
        historial.setPadding(new Insets(0, 12, 12, 12));

        // ---------- Acciones ----------
        btnAgregar.setOnAction(e -> {
            Producto p = tablaCatalogo.getSelectionModel().getSelectedItem();
            if (p == null) { aviso("Seleccione un producto del catálogo."); return; }
            int cant;
            double costo;
            try {
                cant = Integer.parseInt(txtCantidad.getText().trim());
                costo = Double.parseDouble(txtCosto.getText().trim().replace(",", ""));
            } catch (NumberFormatException ex) {
                aviso("Cantidad y costo deben ser números válidos.");
                return;
            }
            if (cant <= 0) { aviso("La cantidad debe ser mayor a 0."); return; }
            if (costo <= 0) { aviso("El costo debe ser mayor a 0."); return; }
            carrito.removeIf(l -> l.getCodigo().equals(p.getCodigo()));
            carrito.add(new LineaCompra(p.getCodigo(), p.getNombre(), cant, costo));
            recalcular.run();
            txtCantidad.setText("1");
            txtCosto.clear();
        });

        btnRegistrar.setOnAction(e -> {
            Proveedor prov = cmbProveedor.getValue();
            if (prov == null) { aviso("Seleccione un proveedor."); return; }
            if (carrito.isEmpty()) { aviso("Agregue al menos un producto."); return; }
            Alert conf = new Alert(AlertType.CONFIRMATION,
                    lblTotal.getText() + "\n¿Registrar la compra? El stock aumentará.", ButtonType.OK, ButtonType.CANCEL);
            conf.setTitle("Confirmar compra");
            if (conf.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return;

            btnRegistrar.setDisable(true);
            try {
                int id = compraDAO.registrar(Sesion.getActual().getId(), prov.getIdProveedor(),
                        txtDocumento.getText(), new java.util.ArrayList<>(carrito));
                new Alert(AlertType.INFORMATION, "Compra COMP-" + id + " registrada. Inventario actualizado.").showAndWait();
                carrito.clear();
                recalcular.run();
                txtDocumento.clear();
                productos.setAll(new ProductoDAO().listar());
                tablaHist.setItems(FXCollections.observableArrayList(compraDAO.historial()));
            } catch (Exception ex) {
                Alert a = new Alert(AlertType.ERROR, ex.getMessage());
                a.setTitle("No se pudo registrar la compra");
                a.showAndWait();
            } finally {
                btnRegistrar.setDisable(false);
            }
        });

        SplitPane split = new SplitPane(izquierda, derecha);
        split.setDividerPositions(0.45);
        VBox.setVgrow(split, Priority.ALWAYS);
        VBox raiz = new VBox(split, historial);
        Platform.runLater(txtBuscar::requestFocus);
        return raiz;
    }

    private static void aviso(String msg) {
        new Alert(AlertType.WARNING, msg).showAndWait();
    }
}
