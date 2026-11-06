package com.puntoventa;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import modelo.FacturaDatos;
import modelo.LineaVenta;

/** Genera la factura en PDF a partir de los datos guardados de la venta. */
public class FacturaPdfGenerator {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color AZUL = new Color(0x1E, 0x3A, 0x8A);
    private static final Color GRIS = new Color(0xF1, 0xF5, 0xF9);

    public static void generar(FacturaDatos f, File destino) throws Exception {
        Document doc = new Document(PageSize.LETTER, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, new FileOutputStream(destino));
        doc.open();

        Font fEmpresa = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, AZUL);
        Font fNormal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font fNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font fEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font fTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, AZUL);
        Font fAnulada = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(0xDC, 0x26, 0x26));

        // ----- Datos del negocio -----
        Paragraph empresa = new Paragraph(f.empresaNombre, fEmpresa);
        empresa.setAlignment(Element.ALIGN_CENTER);
        doc.add(empresa);
        if (!vacio(f.empresaRnc)) {
            centrado(doc, "RNC: " + f.empresaRnc, fNormal);
        }
        if (!vacio(f.empresaDireccion)) {
            centrado(doc, f.empresaDireccion, fNormal);
        }
        if (!vacio(f.empresaTelefono)) {
            centrado(doc, "Tel.: " + f.empresaTelefono, fNormal);
        }
        doc.add(new Paragraph(" "));

        Paragraph titulo = new Paragraph("FACTURA", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);
        if ("ANULADA".equals(f.estado)) {
            Paragraph anulada = new Paragraph("*** FACTURA ANULADA ***", fAnulada);
            anulada.setAlignment(Element.ALIGN_CENTER);
            doc.add(anulada);
        }
        doc.add(new Paragraph(" "));

        // ----- Cabecera de la venta -----
        PdfPTable cabecera = new PdfPTable(2);
        cabecera.setWidthPercentage(100);
        cabecera.addCell(celdaSinBorde("Factura No.: " + f.numeroFactura, fNegrita));
        cabecera.addCell(celdaSinBorde("Fecha: " + f.fecha.format(FORMATO_FECHA), fNormal));
        cabecera.addCell(celdaSinBorde("Cliente: " + f.clienteNombre, fNormal));
        cabecera.addCell(celdaSinBorde("Cédula: " + f.clienteCedula, fNormal));
        cabecera.addCell(celdaSinBorde("Atendido por: " + f.cajero, fNormal));
        cabecera.addCell(celdaSinBorde("Método de pago: " + (f.metodoPago == null ? "-" : f.metodoPago), fNormal));
        doc.add(cabecera);
        doc.add(new Paragraph(" "));

        // ----- Detalle -----
        PdfPTable tabla = new PdfPTable(new float[]{1f, 5f, 2f, 2f});
        tabla.setWidthPercentage(100);
        String[] titulos = {"Cant.", "Descripción", "Precio", "Importe"};
        for (String t : titulos) {
            PdfPCell c = new PdfPCell(new Phrase(t, fEncabezado));
            c.setBackgroundColor(AZUL);
            c.setPadding(5);
            if (!"Descripción".equals(t)) {
                c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
            tabla.addCell(c);
        }
        boolean alterna = false;
        for (LineaVenta l : f.lineas) {
            Color fondo = alterna ? GRIS : Color.WHITE;
            tabla.addCell(celdaTabla(String.valueOf(l.getCantidad()), fNormal, Element.ALIGN_RIGHT, fondo));
            tabla.addCell(celdaTabla(l.getNombre() + "  (" + l.getCodigo() + ")", fNormal, Element.ALIGN_LEFT, fondo));
            tabla.addCell(celdaTabla(dinero(l.getPrecioUnitario()), fNormal, Element.ALIGN_RIGHT, fondo));
            tabla.addCell(celdaTabla(dinero(l.getSubtotal()), fNormal, Element.ALIGN_RIGHT, fondo));
            alterna = !alterna;
        }
        doc.add(tabla);
        doc.add(new Paragraph(" "));

        // ----- Totales (alineados a la derecha) -----
        PdfPTable totales = new PdfPTable(new float[]{6f, 3f});
        totales.setWidthPercentage(60);
        totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
        fila(totales, "Subtotal", dinero(f.subtotal), fNormal);
        fila(totales, "Descuento", "-" + dinero(f.descuento), fNormal);
        fila(totales, "ITBIS", dinero(f.impuesto), fNormal);
        fila(totales, "TOTAL", dinero(f.total), fTotal);
        if (f.efectivoRecibido != null) {
            fila(totales, "Efectivo recibido", dinero(f.efectivoRecibido), fNormal);
            fila(totales, "Cambio", dinero(f.cambio == null ? 0 : f.cambio), fNormal);
        }
        doc.add(totales);

        doc.add(new Paragraph(" "));
        Paragraph gracias = new Paragraph("Gracias por su compra", fNegrita);
        gracias.setAlignment(Element.ALIGN_CENTER);
        doc.add(gracias);

        doc.close();
    }

    /** Abre el PDF con el visor predeterminado del sistema (desde ahí se imprime). */
    public static void abrir(File archivo) throws Exception {
        if (!java.awt.Desktop.isDesktopSupported()) {
            throw new Exception("Este equipo no permite abrir archivos desde la aplicación. Archivo guardado en: "
                    + archivo.getAbsolutePath());
        }
        java.awt.Desktop.getDesktop().open(archivo);
    }

    // ---------- Utilidades ----------

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static String dinero(double valor) {
        return String.format("$%,.2f", valor);
    }

    private static void centrado(Document doc, String texto, Font fuente) throws Exception {
        Paragraph p = new Paragraph(texto, fuente);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    private static PdfPCell celdaSinBorde(String texto, Font fuente) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(3);
        return c;
    }

    private static PdfPCell celdaTabla(String texto, Font fuente, int alineacion, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setHorizontalAlignment(alineacion);
        c.setBackgroundColor(fondo);
        c.setPadding(5);
        return c;
    }

    private static void fila(PdfPTable tabla, String etiqueta, String valor, Font fuente) {
        PdfPCell a = new PdfPCell(new Phrase(etiqueta, fuente));
        a.setBorder(Rectangle.NO_BORDER);
        a.setHorizontalAlignment(Element.ALIGN_RIGHT);
        PdfPCell b = new PdfPCell(new Phrase(valor, fuente));
        b.setBorder(Rectangle.NO_BORDER);
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(a);
        tabla.addCell(b);
    }
}
