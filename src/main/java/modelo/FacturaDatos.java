package modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Todos los datos que se imprimen en una factura (cabecera, líneas y totales). */
public class FacturaDatos {
    public String numeroFactura;
    public LocalDateTime fecha;
    public String clienteNombre;
    public String clienteCedula;
    public String cajero;
    public String metodoPago;
    public String estado;
    public double subtotal;
    public double descuento;
    public double impuesto;
    public double total;
    public Double efectivoRecibido;
    public Double cambio;
    public final List<LineaVenta> lineas = new ArrayList<>();

    // Datos del negocio (tabla "configuracion")
    public String empresaNombre;
    public String empresaRnc;
    public String empresaDireccion;
    public String empresaTelefono;
}
