package com.puntoventa;

import java.io.File;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import com.puntoventa.dao.FacturaDAO;
import modelo.FacturaDatos;

/** Genera la factura en PDF y la abre en el visor (vista previa e impresión). */
public class FacturaVista {

    public static void mostrar(String numeroFactura) {
        try {
            FacturaDatos datos = new FacturaDAO().obtener(numeroFactura);
            File temporal = File.createTempFile("factura_" + numeroFactura + "_", ".pdf");
            temporal.deleteOnExit();
            FacturaPdfGenerator.generar(datos, temporal);
            FacturaPdfGenerator.abrir(temporal);
        } catch (Exception ex) {
            new Alert(AlertType.ERROR, "No se pudo mostrar la factura: " + ex.getMessage()).showAndWait();
        }
    }
}
