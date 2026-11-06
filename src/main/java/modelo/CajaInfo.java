package modelo;

import java.time.LocalDateTime;

/** Datos de una caja (turno de caja) para mostrar en pantalla. */
public class CajaInfo {
    private final int idCaja;
    private final String cajero;
    private final LocalDateTime fechaApertura;
    private final double montoInicial;
    private final LocalDateTime fechaCierre;
    private final Double montoEsperado;
    private final Double montoContado;
    private final Double diferencia;
    private final String estado;

    public CajaInfo(int idCaja, String cajero, LocalDateTime fechaApertura, double montoInicial,
                    LocalDateTime fechaCierre, Double montoEsperado, Double montoContado,
                    Double diferencia, String estado) {
        this.idCaja = idCaja;
        this.cajero = cajero;
        this.fechaApertura = fechaApertura;
        this.montoInicial = montoInicial;
        this.fechaCierre = fechaCierre;
        this.montoEsperado = montoEsperado;
        this.montoContado = montoContado;
        this.diferencia = diferencia;
        this.estado = estado;
    }

    public int getIdCaja() { return idCaja; }
    public String getCajero() { return cajero; }
    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public double getMontoInicial() { return montoInicial; }
    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public Double getMontoEsperado() { return montoEsperado; }
    public Double getMontoContado() { return montoContado; }
    public Double getDiferencia() { return diferencia; }
    public String getEstado() { return estado; }

    // Textos listos para mostrar en la tabla del historial
    public String getAperturaTexto() {
        return fechaApertura == null ? "" : fechaApertura.toString().replace('T', ' ').substring(0, 16);
    }

    public String getCierreTexto() {
        return fechaCierre == null ? "—" : fechaCierre.toString().replace('T', ' ').substring(0, 16);
    }

    public String getEsperadoTexto() {
        return montoEsperado == null ? "—" : String.format("$%,.2f", montoEsperado);
    }

    public String getContadoTexto() {
        return montoContado == null ? "—" : String.format("$%,.2f", montoContado);
    }

    public String getDiferenciaTexto() {
        return diferencia == null ? "—" : String.format("$%,.2f", diferencia);
    }

    public String getInicialTexto() {
        return String.format("$%,.2f", montoInicial);
    }
}
