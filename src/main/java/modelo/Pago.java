package modelo;

public class Pago {
    private int idPago;
    private double monto;
    private String metodo;

    public Pago() {
    }

    public Pago(int idPago, double monto, String metodo) {
        this.idPago = idPago;
        this.monto = monto;
        this.metodo = metodo;
    }

    public int getIdPago() {
        return idPago;
    }

    public void setIdPago(int idPago) {
        this.idPago = idPago;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    @Override
    public String toString() {
        return metodo + " - $" + monto;
    }
}
