package com.ggm.ad.examen_ut6.dao;

import com.ggm.ad.examen_ut6.model.Actor;

import java.util.List;

public interface IActorDao {

    /**
     * Lee del fichero json "actores.json" y devuelve una lista de actores que estén en activo y tengan menos de la edad especificada por parametros.
     * @param edad Edad máxima de los actores a buscar
     * @return Lista de actores que cumplen las condiciones
     */
    List<Actor> obtenerActoresEnActivoMenoresDe(int edad);
}
