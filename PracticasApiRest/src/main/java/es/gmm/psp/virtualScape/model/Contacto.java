package es.gmm.psp.virtualScape.model;

public class Contacto {
    private String titular;
    private int telefono;

    public Contacto(String titular, int telefono) {
        this.titular = titular;
        this.telefono = telefono;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public int getTelefono() {
        return telefono;
    }

    public void setTelefono(int telefono) {
        this.telefono = telefono;
    }

    @Override
    public String toString() {
        return "Contacto{" +
                "titular='" + titular + '\'' +
                ", telefono=" + telefono +
                '}';
    }
}
