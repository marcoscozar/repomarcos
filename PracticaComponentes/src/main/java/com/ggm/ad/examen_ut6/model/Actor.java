package com.ggm.ad.examen_ut6.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "ACTORES")
public class Actor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(nullable = false)
    private int edad;

    @Column(name = "en_activo", nullable = false)
    private boolean enActivo;

    @Column(name = "num_galardones", nullable = false)
    private int numGalardones;

    @OneToMany(mappedBy = "protagonista", cascade = CascadeType.ALL)
    private List<Pelicula> peliculas;

    // Constructores
    public Actor() {}

    public Actor(String nombreCompleto, int edad, boolean enActivo, int numGalardones) {
        this.nombreCompleto = nombreCompleto;
        this.edad = edad;
        this.enActivo = enActivo;
        this.numGalardones = numGalardones;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public boolean isEnActivo() { return enActivo; }
    public void setEnActivo(boolean enActivo) { this.enActivo = enActivo; }

    public int getNumGalardones() { return numGalardones; }
    public void setNumGalardones(int numGalardones) { this.numGalardones = numGalardones; }


    public List<Pelicula> getPeliculas() {
        return peliculas;
    }

    public void setPeliculas(List<Pelicula> peliculas) {
        this.peliculas = peliculas;
    }

    @Override
    public String toString() {
        return "Actor{" +
                "id=" + id +
                ", nombreCompleto='" + nombreCompleto + '\'' +
                ", edad=" + edad +
                ", enActivo=" + enActivo +
                ", numGalardones=" + numGalardones +
                ", peliculas=" + peliculas +
                '}';
    }
}
