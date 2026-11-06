package com.puntoventa;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.puntoventa.dao.CajaDAO;
import modelo.CajaInfo;
import modelo.ResumenCaja;

/**
 * Módulo de Caja: apertura, resumen en vivo, cierre con arqueo (efectivo contado contra
 * efectivo esperado) e historial de cajas.
 */
public class CajaPantalla {

    public static Node crear() {
        VBox contenedor = new VBox(16);
        contenedor.setPadding(new Insets(16));
        refrescar(contenedor);

        ScrollPane scroll = new ScrollPane(contenedor);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        return scroll;
    }

    private static void refrescar(VBox contenedor) {
        contenedor.getChildren().clear();

        int idUsuario = Sesion.getActual().getId();
        boolean esAdmin = Roles.ADMINISTRADOR.equals(Sesion.getRolActual());
        CajaDAO cajaDAO = new CajaDAO();
        CajaInfo caja = cajaDAO.obtenerAbierta(idUsuario);

        Label lblMensaje = new Label();
        lblMensaje.setWrapText(true);

        if (caja == null) {
            contenedor.getChildren().add(crearFormularioApertura(contenedor, cajaDAO, idUsuario, lblMensaje));
        } else {
            contenedor.getChildren().add(crearPanelCajaAbierta(contenedor, cajaDAO, caja, idUsuario, lblMensaje));
        }
        contenedor.getChildren().add(lblMensaje);
        contenedor.getChildren().add(crearHistorial(cajaDAO, idUsuario, esAdmin));
    }

    // ---------------- Apertura ----------------

    private static VBox crearFormularioApertura(VBox contenedor, CajaDAO cajaDAO, int idUsuario, Label lblMensaje) {
        Label titulo = new Label("Abrir caja");
        titulo.getStyleClass().add("label-titulo");
        Label explicacion = new Label("No tienes una caja abierta. Indica el efectivo con el que empiezas el turno para poder vender.");
        explicacion.setWrapText(true);

        TextField txtMonto = new TextField();
        txtMonto.setPromptText("Monto inicial en efectivo");
        txtMonto.setPrefWidth(220);

        Button btnAbrir = new Button("Abrir caja");
        btnAbrir.getStyleClass().add("button-primario");
        btnAbrir.setDefaultButton(true);
        btnAbrir.setOnAction(e -> {
            lblMensaje.getStyleClass().remove("alerta-error");
            lblMensaje.setText("");
            double monto;
            try {
                monto = parseMonto(txtMonto.getText());
            } catch (NumberFormatException ex) {
                mostrarError(lblMensaje, "El monto inicial debe ser un número.");
                return;
            }
            try {
                cajaDAO.abrir(idUsuario, monto);
                refrescar(contenedor);
            } catch (Exception ex) {
                mostrarError(lblMensaje, ex.getMessage());
            }
        });

        HBox fila = new HBox(10, txtMonto, btnAbrir);
        VBox tarjeta = new VBox(10, titulo, explicacion, fila);
        tarjeta.getStyleClass().add("tarjeta-kpi");
        tarjeta.setPadding(new Insets(16));
        return tarjeta;
    }

    // ---------------- Caja abierta: resumen y cierre ----------------

    private static VBox crearPanelCajaAbierta(VBox contenedor, CajaDAO cajaDAO, CajaInfo caja,
                                              int idUsuario, Label lblMensaje) {
        Label titulo = new Label("Caja #" + caja.getIdCaja() + " abierta");
        titulo.getStyleClass().add("label-titulo");

        ResumenCaja resumen = null;
        try {
            resumen = cajaDAO.resumen(caja.getIdCaja());
        } catch (Exception ex) {
            mostrarError(lblMensaje, "No se pudo calcular el resumen: " + ex.getMessage());
        }

        GridPane datos = new GridPane();
        datos.setHgap(24);
        datos.setVgap(8);
        int fila = 0;
        datos.addRow(fila++, new Label("Cajero:"), new Label(caja.getCajero()));
        datos.addRow(fila++, new Label("Abierta desde:"), new Label(caja.getAperturaTexto()));
        datos.addRow(fila++, new Label("Monto inicial:"), new Label(caja.getInicialTexto()));

        double esperado = caja.getMontoInicial();
        if (resumen != null) {
            datos.addRow(fila++, new Label("Entradas netas de efectivo:"),
                    new Label(String.format("$%,.2f", resumen.getMovimientosEfectivo())));
            for (String metodo : resumen.getVentasPorMetodo().keySet()) {
                datos.addRow(fila++, new Label("Ventas con " + metodo.toLowerCase() + ":"),
                        new Label(resumen.getCantidadPorMetodo().get(metodo) + " venta(s) por "
                                + String.format("$%,.2f", resumen.getVentasPorMetodo().get(metodo))));
            }
            esperado = resumen.getMontoEsperado();
        }
        Label lblEsperado = new Label(String.format("$%,.2f", esperado));
        lblEsperado.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        datos.addRow(fila, new Label("Efectivo esperado en caja:"), lblEsperado);

        // ----- Cierre / arqueo -----
        Label tituloCierre = new Label("Cierre y arqueo");
        tituloCierre.getStyleClass().add("label-titulo");
        Label ayuda = new Label("Cuenta el efectivo que hay en la caja y escríbelo. El sistema calcula la diferencia contra lo esperado.");
        ayuda.setWrapText(true);

        TextField txtContado = new TextField();
        txtContado.setPromptText("Efectivo contado");
        txtContado.setPrefWidth(220);
        TextField txtObservacion = new TextField();
        txtObservacion.setPromptText("Observación (opcional)");
        txtObservacion.setPrefWidth(320);

        Button btnCerrar = new Button("Cerrar caja");
        btnCerrar.getStyleClass().add("button-peligro");
        Button btnActualizar = new Button("Actualizar resumen");
        btnActualizar.setOnAction(e -> refrescar(contenedor));

        btnCerrar.setOnAction(e -> {
            lblMensaje.getStyleClass().remove("alerta-error");
            lblMensaje.setText("");
            double contado;
            try {
                contado = parseMonto(txtContado.getText());
            } catch (NumberFormatException ex) {
                mostrarError(lblMensaje, "El efectivo contado debe ser un número.");
                return;
            }
            Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                    "¿Cerrar la caja con " + String.format("$%,.2f", contado) + " contados? Esta acción no se puede deshacer.");
            confirmacion.setTitle("Cerrar caja");
            confirmacion.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                try {
                    double diferencia = cajaDAO.cerrar(caja.getIdCaja(), idUsuario, contado, txtObservacion.getText());
                    String texto = diferencia == 0 ? "La caja cuadra perfectamente."
                            : diferencia > 0 ? "Sobrante de " + String.format("$%,.2f", diferencia) + "."
                            : "Faltante de " + String.format("$%,.2f", -diferencia) + ".";
                    new Alert(AlertType.INFORMATION, "Caja cerrada.\n" + texto).showAndWait();
                    refrescar(contenedor);
                } catch (Exception ex) {
                    mostrarError(lblMensaje, ex.getMessage());
                }
            });
        });

        HBox filaCierre = new HBox(10, txtContado, txtObservacion, btnCerrar, btnActualizar);
        VBox tarjeta = new VBox(12, titulo, datos, tituloCierre, ayuda, filaCierre);
        tarjeta.getStyleClass().add("tarjeta-kpi");
        tarjeta.setPadding(new Insets(16));
        return tarjeta;
    }

    // ---------------- Historial ----------------

    private static VBox crearHistorial(CajaDAO cajaDAO, int idUsuario, boolean esAdmin) {
        Label titulo = new Label(esAdmin ? "Historial de cajas (todas)" : "Historial de mis cajas");
        titulo.getStyleClass().add("label-titulo");

        TableView<CajaInfo> tabla = new TableView<>();
        tabla.setPlaceholder(new Label("Todavía no hay cajas registradas."));
        tabla.getStyleClass().add("table-view");
        tabla.setPrefHeight(280);

        tabla.getColumns().add(columna("#", c -> String.valueOf(c.getIdCaja())));
        tabla.getColumns().add(columna("Cajero", CajaInfo::getCajero));
        tabla.getColumns().add(columna("Apertura", CajaInfo::getAperturaTexto));
        tabla.getColumns().add(columna("Cierre", CajaInfo::getCierreTexto));
        tabla.getColumns().add(columna("Inicial", CajaInfo::getInicialTexto));
        tabla.getColumns().add(columna("Esperado", CajaInfo::getEsperadoTexto));
        tabla.getColumns().add(columna("Contado", CajaInfo::getContadoTexto));
        tabla.getColumns().add(columna("Diferencia", CajaInfo::getDiferenciaTexto));
        tabla.getColumns().add(columna("Estado", CajaInfo::getEstado));
        tabla.setItems(FXCollections.observableArrayList(cajaDAO.historial(idUsuario, esAdmin)));

        return new VBox(8, titulo, tabla);
    }

    private static TableColumn<CajaInfo, String> columna(String titulo, java.util.function.Function<CajaInfo, String> valor) {
        TableColumn<CajaInfo, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        return col;
    }

    // ---------------- Utilidades ----------------

    private static void mostrarError(Label lbl, String texto) {
        if (!lbl.getStyleClass().contains("alerta-error")) {
            lbl.getStyleClass().add("alerta-error");
        }
        lbl.setText(texto);
    }

    /** Acepta "12,500.50" o "12500.50" (quita las comas de miles). */
    private static double parseMonto(String texto) {
        return Double.parseDouble(texto.trim().replace(",", "").replace(" ", ""));
    }
}
