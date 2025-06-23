package es.gmm.psp.virtualScape.repository;

import es.gmm.psp.virtualScape.model.Sala;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface SalaRepository extends MongoRepository<Sala, String> {
    List<Sala> findByTematicas(String name);

    Sala findByNombre(String nombre);

}