package es.gmm.psp.virtualScape.repository;

import es.gmm.psp.virtualScape.model.Reserva;
import es.gmm.psp.virtualScape.model.RespuestaEspecial;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Objects;

public interface ReservaRepository extends MongoRepository<Reserva, String> {
    List<Reserva> findByFechaDiaReserva(int diaReserva);

}
