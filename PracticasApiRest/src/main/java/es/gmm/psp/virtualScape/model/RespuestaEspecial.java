package es.gmm.psp.virtualScape.model;

public class RespuestaEspecial {
    private String sala;
    private int totalReservas;

    public RespuestaEspecial(String sala, int totalReservas) {
        this.sala = sala;
        this.totalReservas = totalReservas;
    }
    public String getSala() {
        return sala;
    }

    public void setSala(String sala) {
        this.sala = sala;
    }

    public int getTotalReservas() {
        return totalReservas;
    }

    public void setTotalReservas(int totalReservas) {
        this.totalReservas = totalReservas;
    }
}
