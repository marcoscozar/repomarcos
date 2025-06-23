package com.ggm.ad.examen_ut6.dao.impl;

import com.ggm.ad.examen_ut6.ConnectionManager;
import com.ggm.ad.examen_ut6.dao.IPeliculaDao;
import com.ggm.ad.examen_ut6.model.Pelicula;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Component
public class PeliculaDao implements IPeliculaDao {
    
    @Autowired
    private ConnectionManager connectionManager;

    @Override
    public List<Pelicula> consultarPeliculasDeActor(String nombreActor) {
        List<Pelicula> peliculas = new ArrayList<>();
        String sql = "SELECT * FROM peliculas WHERE protagonista_id = (SELECT id FROM actores WHERE nombre_completo = ?)";
        
        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nombreActor);
            ResultSet resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                Pelicula pelicula = new Pelicula();
                pelicula.setId(resultSet.getInt("id"));
                pelicula.setTitulo(resultSet.getString("titulo"));
                pelicula.setAnio(resultSet.getInt("anio"));
                pelicula.setImdbRating(resultSet.getDouble("imdb_rating"));
                peliculas.add(pelicula);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return peliculas;
    }




}
