package modelo;

import java.util.List;

/** Resultado genérico de un reporte: columnas, filas de texto y un resumen final. */
public class ReporteTabla {
    private final String titulo;
    private final String periodo;
    private final List<String> columnas;
    private final List<String[]> filas;
    private final String resumen;

    public ReporteTabla(String titulo, String periodo, List<String> columnas, List<String[]> filas, String resumen) {
        this.titulo = titulo;
        this.periodo = periodo;
        this.columnas = columnas;
        this.filas = filas;
        this.resumen = resumen;
    }

    public String getTitulo() { return titulo; }
    public String getPeriodo() { return periodo; }
    public List<String> getColumnas() { return columnas; }
    public List<String[]> getFilas() { return filas; }
    public String getResumen() { return resumen; }
}
