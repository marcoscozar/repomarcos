package com.ggm.ad.examen_ut6.dao.impl;

import com.ggm.ad.examen_ut6.ConnectionManager;
import com.ggm.ad.examen_ut6.dao.IAuditoriaDao;
import com.ggm.ad.examen_ut6.model.Actor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Component
public class AuditoriaDao implements IAuditoriaDao {

    @Autowired
    private ConnectionManager connectionManager;

    @Override
    public void registrarVolcado(List<Actor> actores) {
        String sql = "INSERT INTO auditoria_migracion (actores_involucrados, cantidad_peliculas) VALUES (?, ?)";
        StringBuilder actoresInvolucrados = new StringBuilder();
        int cantidadPeliculas = 0;

        for (Actor actor : actores) {
            if (actoresInvolucrados.length() > 0) {
                actoresInvolucrados.append(", ");
            }
            actoresInvolucrados.append(actor.getNombreCompleto());
            cantidadPeliculas += actor.getPeliculas().size();
        }

        try (Connection connection = connectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, actoresInvolucrados.toString());
            statement.setInt(2, cantidadPeliculas);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
