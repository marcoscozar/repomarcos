package es.gmm.psp.virtualScape;

import es.gmm.psp.virtualScape.model.Contacto;
import es.gmm.psp.virtualScape.model.Fecha;
import es.gmm.psp.virtualScape.model.Reserva;
import es.gmm.psp.virtualScape.model.Sala;
import es.gmm.psp.virtualScape.service.ReservaService;
import es.gmm.psp.virtualScape.service.SalaService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VirtualScapeApplication {

	public static void main(String[] args) {
		var context = SpringApplication.run(VirtualScapeApplication.class, args);
		VirtualScapeApplication app = context.getBean(VirtualScapeApplication.class);

		ReservaService reservaService = context.getBean(ReservaService.class);
		SalaService salaService=context.getBean(SalaService.class);



	}

}
