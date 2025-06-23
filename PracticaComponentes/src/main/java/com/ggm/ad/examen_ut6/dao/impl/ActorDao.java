package com.ggm.ad.examen_ut6.dao.impl;

import com.ggm.ad.examen_ut6.ConnectionManager;
import com.ggm.ad.examen_ut6.dao.IActorDao;
import com.ggm.ad.examen_ut6.model.Actor;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ActorDao implements IActorDao {
    
    @Value("${ruta.archivo.actores}")
    private String rutaArchivo;
    
    @Override
    public List<Actor> obtenerActoresEnActivoMenoresDe(int edad) {
        Gson gson = new Gson();
        List<Actor> actores = new ArrayList<>();
        try (FileReader reader = new FileReader(rutaArchivo)) {
            Actor[] actoresArray = gson.fromJson(reader, Actor[].class);
            for(Actor actor : actoresArray) {
                if(actor.isEnActivo() && actor.getEdad() < edad) {
                    System.out.println(actor);
                    actores.add(actor);
                }
            }
            return actores;
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

}
