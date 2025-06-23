package es.gmm.psp.virtualScape.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "salas")
public class Sala {
    @Id
    private String id;
    private String nombre;
    private int capacidadMin;
    private int capacidadMax;
    private List<String> tematicas;

    public Sala() {
    }

    public Sala(String nombre, int capacidadMin, int capacidadMax, List<String> tematicas) {
        this.nombre = nombre;
        this.capacidadMin = capacidadMin;
        this.capacidadMax = capacidadMax;
        this.tematicas = tematicas;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCapacidadMin() {
        return capacidadMin;
    }

    public void setCapacidadMin(int capacidadMin) {
        this.capacidadMin = capacidadMin;
    }

    public int getCapacidadMax() {
        return capacidadMax;
    }

    public void setCapacidadMax(int capacidadMax) {
        this.capacidadMax = capacidadMax;
    }

    public List<String> getTematicas() {
        return tematicas;
    }

    public void setTematicas(List<String> tematicas) {
        this.tematicas = tematicas;
    }

    @Override
    public String toString() {
        return "Sala{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", capacidadMin=" + capacidadMin +
                ", capacidadMax=" + capacidadMax +
                ", tematicas=" + tematicas +
                '}';
    }
}
