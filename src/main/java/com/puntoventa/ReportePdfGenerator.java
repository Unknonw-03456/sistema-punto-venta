package com.puntoventa;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

import com.puntoventa.dao.ReporteDAO;
import com.puntoventa.dao.ReporteDAO.FilaCliente;
import com.puntoventa.dao.ReporteDAO.FilaProducto;
import com.puntoventa.dao.UsuarioDAO;
import modelo.UsuarioAdmin;

/**
 * Genera el informe en PDF. El contenido varía según el rol de quien lo genera:
 * - Operador: productos sin precio, clientes solo con cédula y nombre (sin datos de contacto).
 * - Supervisor: productos y clientes completos.
 * - Administrador: productos y clientes completos, más el listado de usuarios del sistema.
 */
public class ReportePdfGenerator {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color AZUL_ENCABEZADO = new Color(0x1E, 0x3A, 0x8A);

    public static void generar(File archivoDestino, LocalDate desde, LocalDate hasta) throws Exception {
        String rol = Sesion.getRolActual();
        String generadoPor = Sesion.getActual().getNombre() + " (" + rol + ")";
        boolean esOperador = "Operador".equals(rol);
        boolean esAdmin = "Administrador".equals(rol);

        Document documento = new Document(PageSize.LETTER, 40, 40, 50, 40);
        PdfWriter.getInstance(documento, new FileOutputStream(archivoDestino));
        documento.open();

        Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font fuenteSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font fuenteEncabezadoTabla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

        Paragraph titulo = new Paragraph("Sistema Punto de Venta - Informe de Productos y Clientes", fuenteTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        documento.add(new Paragraph(" "));
        documento.add(new Paragraph("Generado por: " + generadoPor, fuenteNormal));
        documento.add(new Paragraph("Fecha de generación: " + LocalDate.now().format(FORMATO_FECHA), fuenteNormal));
        documento.add(new Paragraph("Rango filtrado: " + desde.format(FORMATO_FECHA) + " al " + hasta.format(FORMATO_FECHA), fuenteNormal));
        documento.add(new Paragraph(" "));

        ReporteDAO dao = new ReporteDAO();

        // ===== Productos =====
        documento.add(new Paragraph("Productos registrados", fuenteSeccion));
        documento.add(new Paragraph(" "));
        List<FilaProducto> productos = dao.obtenerProductos(desde, hasta);
        if (productos.isEmpty()) {
            documento.add(new Paragraph("No hay productos registrados en ese rango.", fuenteNormal));
        } else {
            PdfPTable tablaProductos = esOperador ? new PdfPTable(3) : new PdfPTable(4);
            tablaProductos.setWidthPercentage(100);
            agregarEncabezado(tablaProductos, fuenteEncabezadoTabla,
                    esOperador ? new String[]{"Código", "Nombre", "Stock"} : new String[]{"Código", "Nombre", "Precio", "Stock"});
            for (FilaProducto p : productos) {
                tablaProductos.addCell(new Phrase(p.codigo, fuenteNormal));
                tablaProductos.addCell(new Phrase(p.nombre, fuenteNormal));
                if (!esOperador) {
                    tablaProductos.addCell(new Phrase(String.format("%.2f", p.precio), fuenteNormal));
                }
                tablaProductos.addCell(new Phrase(String.valueOf(p.stock), fuenteNormal));
            }
            documento.add(tablaProductos);
        }
        documento.add(new Paragraph(" "));

        // ===== Clientes =====
        documento.add(new Paragraph("Clientes registrados", fuenteSeccion));
        documento.add(new Paragraph(" "));
        List<FilaCliente> clientes = dao.obtenerClientes(desde, hasta);
        if (clientes.isEmpty()) {
            documento.add(new Paragraph("No hay clientes registrados en ese rango.", fuenteNormal));
        } else {
            PdfPTable tablaClientes = esOperador ? new PdfPTable(2) : new PdfPTable(4);
            tablaClientes.setWidthPercentage(100);
            agregarEncabezado(tablaClientes, fuenteEncabezadoTabla,
                    esOperador ? new String[]{"Cédula", "Nombre"} : new String[]{"Cédula", "Nombre", "Teléfono", "Email"});
            for (FilaCliente c : clientes) {
                tablaClientes.addCell(new Phrase(c.cedula, fuenteNormal));
                tablaClientes.addCell(new Phrase(c.nombre, fuenteNormal));
                if (!esOperador) {
                    tablaClientes.addCell(new Phrase(c.telefono, fuenteNormal));
                    tablaClientes.addCell(new Phrase(c.email, fuenteNormal));
                }
            }
            documento.add(tablaClientes);
        }

        // ===== Usuarios (solo Administrador) =====
        if (esAdmin) {
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Usuarios del sistema", fuenteSeccion));
            documento.add(new Paragraph(" "));
            List<UsuarioAdmin> usuarios = new UsuarioDAO().listarUsuariosAdmin();
            PdfPTable tablaUsuarios = new PdfPTable(5);
            tablaUsuarios.setWidthPercentage(100);
            agregarEncabezado(tablaUsuarios, fuenteEncabezadoTabla,
                    new String[]{"Usuario", "Nombre", "Cargo", "Rol", "Estado"});
            for (UsuarioAdmin u : usuarios) {
                tablaUsuarios.addCell(new Phrase(u.getUsuario(), fuenteNormal));
                tablaUsuarios.addCell(new Phrase(u.getNombre(), fuenteNormal));
                tablaUsuarios.addCell(new Phrase(u.getCargo() == null ? "" : u.getCargo(), fuenteNormal));
                tablaUsuarios.addCell(new Phrase(u.getRolNombre(), fuenteNormal));
                tablaUsuarios.addCell(new Phrase(u.getEstado(), fuenteNormal));
            }
            documento.add(tablaUsuarios);
        }

        documento.close();
    }

    private static void agregarEncabezado(PdfPTable tabla, Font fuenteBlanca, String[] columnas) {
        for (String col : columnas) {
            PdfPCell celda = new PdfPCell(new Phrase(col, fuenteBlanca));
            celda.setBackgroundColor(AZUL_ENCABEZADO);
            celda.setPadding(5);
            tabla.addCell(celda);
        }
    }
}
