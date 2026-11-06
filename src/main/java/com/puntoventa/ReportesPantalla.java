package com.puntoventa;

import java.io.File;
import java.time.LocalDate;

import com.puntoventa.dao.ReportePOSDAO;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import modelo.ReporteTabla;

/** Pantalla de reportes del POS: elegir tipo y rango, ver en tabla y exportar a PDF. */
public class ReportesPantalla {

    private static ReporteTabla actual;

    public static Node crear() {
        ReportePOSDAO dao = new ReportePOSDAO();
        String rol = Sesion.getRolActual();

        ComboBox<String> cmbTipo = new ComboBox<>();
        cmbTipo.getItems().addAll(ReportePOSDAO.disponiblesPara(rol));
        cmbTipo.getSelectionModel().selectFirst();

        DatePicker dpDesde = new DatePicker(LocalDate.now().withDayOfMonth(1));
        DatePicker dpHasta = new DatePicker(LocalDate.now());

        Button btnGenerar = new Button("Generar");
        btnGenerar.getStyleClass().add("button-primario");
        Button btnPdf = new Button("Exportar PDF");
        btnPdf.getStyleClass().add("button-acento");
        btnPdf.setDisable(true);

        TableView<String[]> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Elige un reporte y pulsa Generar"));
        VBox.setVgrow(tabla, Priority.ALWAYS);
        Label lblResumen = new Label("");
        lblResumen.getStyleClass().add("label-titulo");

        actual = null;

        btnGenerar.setOnAction(e -> {
            try {
                ReporteTabla r = dao.generar(cmbTipo.getValue(), dpDesde.getValue(), dpHasta.getValue(),
                        Sesion.getActual().getId(), rol);
                actual = r;
                tabla.getColumns().clear();
                for (int i = 0; i < r.getColumnas().size(); i++) {
                    final int idx = i;
                    TableColumn<String[], String> c = new TableColumn<>(r.getColumnas().get(i));
                    c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()[idx]));
                    tabla.getColumns().add(c);
                }
                tabla.getItems().setAll(r.getFilas());
                lblResumen.setText(r.getResumen());
                btnPdf.setDisable(false);
            } catch (Exception ex) {
                new Alert(AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });

        btnPdf.setOnAction(e -> {
            if (actual == null) return;
            FileChooser fc = new FileChooser();
            fc.setTitle("Guardar reporte");
            fc.setInitialFileName("reporte_" + actual.getTitulo().toLowerCase().replace(' ', '_') + "_" + LocalDate.now() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File destino = fc.showSaveDialog(tabla.getScene().getWindow());
            if (destino == null) return;
            try {
                ReporteTablaPdf.generar(actual, Sesion.getActual().getNombre(), destino);
                FacturaPdfGenerator.abrir(destino);
            } catch (Exception ex) {
                new Alert(AlertType.ERROR, "No se pudo crear el PDF: " + ex.getMessage()).showAndWait();
            }
        });

        HBox barra = new HBox(8, new Label("Reporte:"), cmbTipo, new Label("Desde:"), dpDesde,
                new Label("Hasta:"), dpHasta, btnGenerar, btnPdf);
        barra.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("Reportes");
        titulo.getStyleClass().add("label-titulo");
        VBox raiz = new VBox(8, titulo, barra, tabla, lblResumen);
        raiz.setPadding(new Insets(12));
        return raiz;
    }
}
