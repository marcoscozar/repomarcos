package es.gmm.psp.virtualScape.service;

import es.gmm.psp.virtualScape.model.Reserva;
import es.gmm.psp.virtualScape.repository.ReservaRepository;
import es.gmm.psp.virtualScape.repository.SalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private SalaRepository salaRepository;

    public List<Reserva> findAll(){
        return reservaRepository.findAll();
    }

    public Reserva findById(String id){
        return reservaRepository.findById(id).orElse(null);
    }

    public Reserva save(Reserva reserva){
        return reservaRepository.save(reserva);
    }

    public boolean verificarConflicto(Reserva reserva){
        List<Reserva> reservas = findAll();
        for(Reserva r : reservas){
            if(r.getFecha().getDiaReserva() == reserva.getFecha().getDiaReserva() &&
                    r.getFecha().getHoraReserva() == reserva.getFecha().getHoraReserva()){
                return true;
            }
        }
        return false;
    }

    public List<Reserva> obtenerReservasPorDia(int numDia) {
        return reservaRepository.findByFechaDiaReserva(numDia);
    }


    public Reserva actualizarReserva(Reserva reserva) {
        if (reserva == null || reserva.getId() == null) {
            return null;
        }

        return reservaRepository.save(reserva);
    }

    public void eliminarReserva(String id){
        reservaRepository.deleteById(id);
    }
}
