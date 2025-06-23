package es.gmm.psp.virtualScape.service;

import es.gmm.psp.virtualScape.model.Reserva;
import es.gmm.psp.virtualScape.model.RespuestaEspecial;
import es.gmm.psp.virtualScape.model.Sala;
import es.gmm.psp.virtualScape.repository.ReservaRepository;
import es.gmm.psp.virtualScape.repository.SalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SalaService {

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private ReservaRepository reservaRepository;


    public Sala crearSala(Sala sala) {
        return salaRepository.save(sala);
    }

    public List<Sala> findAll() {
        return salaRepository.findAll();
    }

    public Sala findById(String id) {
        return salaRepository.findById(id).orElse(null);
    }

    public List<Sala> findByTematica(String tematica) {
        return salaRepository.findByTematicas(tematica);
    }

    public Sala getByNombre(String nombre) {
        return salaRepository.findByNombre(nombre);
    }

    public List<RespuestaEspecial> obtenerSalasMasReservadas() {
        List<Reserva> reservas = reservaRepository.findAll();
        Map<String, Integer> conteoReservas = new HashMap<>();

        for (Reserva reserva : reservas) {
            String nombreSala = reserva.getNombreSala();
            conteoReservas.put(nombreSala, conteoReservas.getOrDefault(nombreSala, 0) + 1);
        }
        List<Map.Entry<String, Integer>> listaOrdenada = new ArrayList<>(conteoReservas.entrySet());
        listaOrdenada.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        List<RespuestaEspecial> resultado = new ArrayList<>();
        int limite = Math.min(listaOrdenada.size(), 2);

        for (int i = 0; i < limite; i++) {
            Map.Entry<String, Integer> entrada = listaOrdenada.get(i);
            resultado.add(new RespuestaEspecial(entrada.getKey(), entrada.getValue()));
        }

        return resultado;
    }

    public int getTotalJugadores() {
        List<Sala> salas = salaRepository.findAll();
        int totalJugadores = 0;
        for (Sala sala : salas) {
            totalJugadores += sala.getCapacidadMax();
        }
        return totalJugadores;
    }

    public Sala actualizarSala(Sala sala) {
        if (sala == null || sala.getId() == null) {
            throw new IllegalArgumentException("Sala inválida");
        }
        return salaRepository.save(sala);
    }


}
