package com.puntoventa;

import com.puntoventa.dao.AnulacionDAO;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import modelo.VentaResumen;

/** Historial de ventas: buscar, reimprimir factura y anular (solo Administrador/Supervisor). */
public class HistorialVentasPantalla {

    public static Node crear() {
        AnulacionDAO dao = new AnulacionDAO();
        ObservableList<VentaResumen> datos = FXCollections.observableArrayList(dao.historial());
        FilteredList<VentaResumen> filtrados = new FilteredList<>(datos, v -> true);

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por factura, cliente, cajero o estado...");
        txtBuscar.textProperty().addListener((o, a, texto) -> {
            String t = texto == null ? "" : texto.trim().toLowerCase();
            filtrados.setPredicate(v -> t.isEmpty()
                    || v.getNumeroFactura().toLowerCase().contains(t)
                    || v.getCliente().toLowerCase().contains(t)
                    || v.getCajero().toLowerCase().contains(t)
                    || v.getEstado().toLowerCase().contains(t));
        });

        TableView<VentaResumen> tabla = new TableView<>(filtrados);
        tabla.getColumns().add(col("Factura", "numeroFactura"));
        tabla.getColumns().add(col("Fecha", "fechaTexto"));
        tabla.getColumns().add(col("Cliente", "cliente"));
        tabla.getColumns().add(col("Cajero", "cajero"));
        tabla.getColumns().add(col("Pago", "metodo"));
        tabla.getColumns().add(col("Total", "totalTexto"));
        tabla.getColumns().add(col("Estado", "estado"));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("No hay ventas"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button btnVer = new Button("Ver / imprimir factura");
        btnVer.getStyleClass().add("button-primario");
        btnVer.setOnAction(e -> {
            VentaResumen v = tabla.getSelectionModel().getSelectedItem();
            if (v == null) { aviso("Seleccione una venta."); return; }
            FacturaVista.mostrar(v.getNumeroFactura());
        });

        Button btnAnular = new Button("Anular venta");
        btnAnular.getStyleClass().add("button-peligro");
        boolean permitido = AnulacionDAO.puedeAnular(Sesion.getRolActual());
        btnAnular.setVisible(permitido);
        btnAnular.setManaged(permitido);
        btnAnular.setOnAction(e -> {
            VentaResumen v = tabla.getSelectionModel().getSelectedItem();
            if (v == null) { aviso("Seleccione una venta."); return; }
            if ("ANULADA".equals(v.getEstado())) { aviso("Esa venta ya está anulada."); return; }
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Anular venta");
            dlg.setHeaderText("Anular " + v.getNumeroFactura() + " por " + v.getTotalTexto());
            dlg.setContentText("Motivo:");
            dlg.showAndWait().ifPresent(motivo -> {
                try {
                    dao.anular(Sesion.getActual().getId(), Sesion.getRolActual(), v.getNumeroFactura(), motivo);
                    new Alert(AlertType.INFORMATION, "Venta anulada. Stock devuelto al inventario.").showAndWait();
                    datos.setAll(dao.historial());
                } catch (Exception ex) {
                    new Alert(AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            });
        });

        Button btnRefrescar = new Button("Actualizar");
        btnRefrescar.setOnAction(e -> datos.setAll(dao.historial()));

        HBox barra = new HBox(8, txtBuscar, btnRefrescar, btnVer, btnAnular);
        barra.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(txtBuscar, Priority.ALWAYS);

        Label titulo = new Label("Historial de ventas (últimas 200)");
        titulo.getStyleClass().add("label-titulo");
        VBox raiz = new VBox(8, titulo, barra, tabla);
        raiz.setPadding(new Insets(12));
        return raiz;
    }

    private static TableColumn<VentaResumen, String> col(String titulo, String prop) {
        TableColumn<VentaResumen, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        return c;
    }

    private static void aviso(String msg) {
        new Alert(AlertType.WARNING, msg).showAndWait();
    }
}
