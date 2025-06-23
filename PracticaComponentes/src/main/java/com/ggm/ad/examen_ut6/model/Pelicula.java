package com.ggm.ad.examen_ut6.model;

import jakarta.persistence.*;


@Entity
@Table(name = "PELICULAS")
public class Pelicula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false, name = "imdb_rating")
    private double imdbRating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protagonista_id")
    private Actor protagonista;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public double getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(double imdbRating) {
        this.imdbRating = imdbRating;
    }

    public Actor getProtagonista() {
        return protagonista;
    }

    public void setProtagonista(Actor protagonista) {
        this.protagonista = protagonista;
    }

    @Override
    public String toString() {
        return "Pelicula{" +
                "id=" + id +
                ", titulo='" + titulo + '\'' +
                ", anio=" + anio +
                ", imdbRating=" + imdbRating +
                ", actor=" + protagonista.getNombreCompleto() +
                '}';
    }
}