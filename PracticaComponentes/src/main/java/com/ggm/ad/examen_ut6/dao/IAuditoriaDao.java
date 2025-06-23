package com.ggm.ad.examen_ut6.dao;

import com.ggm.ad.examen_ut6.model.Actor;
import com.ggm.ad.examen_ut6.model.Pelicula;

import java.util.List;

public interface IAuditoriaDao {

    /**
     * Registra un volcado de actores en la BD, en tabla "auditoria_migracion".
     * @param actores Lista de actores a registrar, con información de sus películas
     */
    void registrarVolcado(List<Actor> actores);

}
