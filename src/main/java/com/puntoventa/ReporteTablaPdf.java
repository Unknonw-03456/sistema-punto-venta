package com.puntoventa;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.puntoventa.dao.ConfiguracionDAO;
import com.puntoventa.database.ConexionBD;
import java.awt.Color;
import modelo.ReporteTabla;

/** Exporta cualquier ReporteTabla a PDF (horizontal, con encabezado, tabla y resumen). */
public class ReporteTablaPdf {

    public static void generar(ReporteTabla r, String generadoPor, File destino) throws Exception {
        String empresa = "Refrigeración Dominicana";
        try {
            empresa = ConfiguracionDAO.obtener(ConexionBD.getConexion(), "empresa_nombre", empresa);
        } catch (Exception ignorada) {
            // se usa el nombre por defecto
        }

        Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font fSub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font fCelda = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font fResumen = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try (FileOutputStream out = new FileOutputStream(destino)) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph(empresa, fTitulo));
            doc.add(new Paragraph(r.getTitulo() + "  |  " + r.getPeriodo(), fSub));
            doc.add(new Paragraph("Generado por " + generadoPor + " el "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fSub));
            doc.add(new Paragraph(" "));

            int n = r.getColumnas().size();
            PdfPTable tabla = new PdfPTable(n);
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            for (String c : r.getColumnas()) {
                PdfPCell cell = new PdfPCell(new Phrase(c, fHead));
                cell.setBackgroundColor(new Color(31, 78, 121));
                cell.setPadding(5);
                tabla.addCell(cell);
            }
            boolean alterna = false;
            for (String[] fila : r.getFilas()) {
                for (String valor : fila) {
                    PdfPCell cell = new PdfPCell(new Phrase(valor == null ? "" : valor, fCelda));
                    cell.setPadding(4);
                    if (alterna) cell.setBackgroundColor(new Color(240, 244, 248));
                    tabla.addCell(cell);
                }
                alterna = !alterna;
            }
            if (r.getFilas().isEmpty()) {
                PdfPCell vacio = new PdfPCell(new Phrase("Sin datos en el período seleccionado", fCelda));
                vacio.setColspan(n);
                vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
                vacio.setPadding(10);
                tabla.addCell(vacio);
            }
            doc.add(tabla);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(r.getResumen(), fResumen));
            doc.close();
        }
    }
}
