package com.ggm.ad.examen_ut6;

import com.ggm.ad.examen_ut6.service.ServicioMigracionDatos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExamenUt6Application {
	@Autowired
	private ServicioMigracionDatos migracion;

	public static void main(String[] args) {

		var context = SpringApplication.run(ExamenUt6Application.class, args);
		ExamenUt6Application app = context.getBean(ExamenUt6Application.class);

		app.run();
	}

	public void run() {
		System.out.println("**** Iniciando migración de datos... ***");
		migracion.ejecutarMigracion();
	}

}
