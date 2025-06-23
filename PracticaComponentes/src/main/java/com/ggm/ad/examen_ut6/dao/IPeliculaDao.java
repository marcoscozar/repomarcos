package com.ggm.ad.examen_ut6.dao;

import com.ggm.ad.examen_ut6.model.Actor;
import com.ggm.ad.examen_ut6.model.Pelicula;

import java.util.List;

public interface IPeliculaDao {

    /**
     * Consulta las películas en las que ha participado un actor como protagonista.
     * Este método lanzará una consulta en HQL para obtener las películas en las que ha participado el actor como protagonista
     * @param nombreActor Nombre completo del actor
     * @return Lista de películas en las que ha participado el actor como protagonista
     */
    List<Pelicula> consultarPeliculasDeActor(String nombreActor);

}
