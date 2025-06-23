package es.gmm.psp.virtualScape.controller;

import es.gmm.psp.virtualScape.model.*;
import es.gmm.psp.virtualScape.repository.SalaRepository;
import es.gmm.psp.virtualScape.service.ReservaService;
import es.gmm.psp.virtualScape.service.SalaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.apache.juli.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@RestController
@RequestMapping("api/virtual-escape/reservas")
public class ApiReserva {

    @Autowired
    private ReservaService reservaService;
    
    @Autowired
    private SalaService salaService;

    private static final Logger logger = LoggerFactory.getLogger(ApiReserva.class);



    // POST respuestas
    @Operation(summary = "Crear una nueva reserva", description = "Crea una nueva reserva si no hay conflicto de horarios y cumple con las reglas de capacidad")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Reserva creada correctamente",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": true, \"mensaje\": \"Reserva creada correctamente\", \"idGenerado\": \"123456789\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": false, \"mensaje\": \"Los datos enviados no son válidos: nombre de sala no encontrado\", \"idGenerado\": null}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto: choque de horarios con otra reserva",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": false, \"mensaje\": \"Conflicto de horarios: ya existe una reserva para ese horario\", \"idGenerado\": null}"
                            )
                    )
            )
    })
    @PostMapping
    public ResponseEntity<Respuesta> crearReserva(@RequestParam String nombreSala,
                                                  @RequestParam int diaReserva,
                                                  @RequestParam int horaReserva,
                                                  @RequestParam String titular,
                                                  @RequestParam int telefono,
                                                  @RequestParam int jugadores,
                                                  @Valid @RequestBody Reserva reserva) {

        Sala sala = salaService.getByNombre(nombreSala);
        if (sala == null) {
            logger.error("Los datos enviados no son válidos: nombre de la sala no encontrada");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Respuesta(false, "Los datos enviados no son válidos: nombre de sala no encontrado", null));
        }

        if (jugadores > sala.getCapacidadMax()) {
            logger.error("El número de jugadores excede la capacidad máxima de la sala");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Respuesta(false, "El número de jugadores excede la capacidad máxima de la sala", null));
        }

        if (jugadores < sala.getCapacidadMin()) {
            logger.error("El número de jugadores no supera la capacidad mínima de la sala");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Respuesta(false, "El número de jugadores no supera la capacidad mínima de la sala", null));
        }

        Reserva nuevaReserva = new Reserva(nombreSala,
                new Fecha(diaReserva, horaReserva),
                new Contacto(titular, telefono),
                jugadores);

        boolean hayConflicto = reservaService.verificarConflicto(nuevaReserva);
        if (hayConflicto) {
            logger.error("Conflicto de horarios con otra reserva");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Respuesta(false, "Conflicto de horarios con otra reserva", null));
        }

        nuevaReserva = reservaService.save(nuevaReserva);
        logger.info("Reserva creada con éxito");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Respuesta(true, "Reserva creada con éxito", nuevaReserva.getId()));
    }


    // GET reservas
    @Operation(summary = "Obtener todas las reservas", description = "Devuelve una lista de todas las reservas")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de reservas encontrada",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(
                                            example = "[{\"id\": \"1\", \"nombreSala\": \"Sala A\", \"fecha\": \"2025-02-20T10:00:00\", \"horaInicio\": \"10:00\", \"horaFin\": \"12:00\"}, {\"id\": \"2\", \"nombreSala\": \"Sala B\", \"fecha\": \"2025-02-20T12:00:00\", \"horaInicio\": \"12:00\", \"horaFin\": \"14:00\"}]"
                                    )
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "204",
                    description = "No hay reservas registradas",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": false, \"mensaje\": \"No hay reservas registradas\", \"idGenerado\": null}"
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<?> getReservas() {
        List<Reserva> reservas = reservaService.findAll();
        if (reservas == null || reservas.isEmpty()) {
            logger.error("No hay reservas registradas");
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new Respuesta(false, "No hay reservas registradas", null));
        }
        logger.info("Reservas encontradas");
        return ResponseEntity.ok(reservas);
    }

    // GET reservas/id
    @Operation(summary = "Obtener detalles de una reserva", description = "Devuelve los detalles de una reserva específica dado su identificador")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reserva encontrada",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"sala\": \"La Casa del Terror\", \"dia\": 3, \"hora\": 18, \"contacto\": { \"titular\": \"Diego\", \"telefono\": 685414005 }, \"jugadores\": 4 }"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reserva no encontrada",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": false, \"mensaje\": \"Reserva no encontrada\", \"idGenerado\": null}"
                            )
                    )
            )

    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getReservaById(@PathVariable String id) {
        Reserva reserva = reservaService.findById(id);
        if (reserva != null) {
            logger.info("Reserva encontrada");
            return ResponseEntity.ok(reserva);
        }
        logger.error("Reserva no encontrada");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Respuesta(false, "Reserva no encontrada", null));
    }

    // PUT reservas/id
    @Operation(summary = "Actualizar una reserva", description = "Actualiza de forma completa una reserva existente con los nuevos datos")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reserva actualizada correctamente",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": true, \"mensaje\": \"Reserva actualizada correctamente\", \"idGenerado\": null}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos en la petición",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": false, \"mensaje\": \"Datos inválidos en la petición\", \"idGenerado\": null}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reserva no encontrada",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": false, \"mensaje\": \"Reserva no encontrada\", \"idGenerado\": null}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto: choque de horarios con otra reserva",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": false, \"mensaje\": \"Conflicto de horarios con otra reserva\", \"idGenerado\": null}"

                            )
                    )
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<Respuesta> actualizarReserva(@PathVariable String id,
       @RequestParam String nombreSala,
       @RequestParam int diaReserva,
       @RequestParam int horaReserva,
       @RequestParam String titular,
       @RequestParam int telefono,
       @RequestParam int jugadores,
       @Valid @RequestBody Reserva reserva) {
        Reserva reservaExistente = reservaService.findById(id);
        if (reservaExistente == null) {
            logger.error("Reserva no encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Respuesta(false, "Reserva no encontrada", null));
        }

        Sala salaExistente = salaService.getByNombre(nombreSala);
        if (salaExistente == null) {
            logger.error("La sala especificada no existe");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Respuesta(false, "La sala especificada no existe", null));
        }

        reserva.setId(id); // Asegurar que se usa el ID correcto
        boolean hayConflicto = reservaService.verificarConflicto(reserva);
        if (hayConflicto) {
            logger.error("Conflicto de horarios con otra reserva");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Respuesta(false, "Conflicto de horarios con otra reserva", null));
        }

        reserva.setNombreSala(nombreSala);
        reserva.setFecha(new Fecha(diaReserva, horaReserva));
        reserva.setContacto(new Contacto(titular, telefono));
        reserva.setJugadores(jugadores);

        try {
            Reserva reservaActualizada = reservaService.actualizarReserva(reserva);
            Reserva verificacion = reservaService.findById(reservaActualizada.getId());
            if (verificacion == null) {
                logger.error("Error al verificar la actualización");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new Respuesta(false, "Error al verificar la actualización", null));
            }
            logger.info("Reserva actualizada con éxito");
            return ResponseEntity.ok(new Respuesta(true, "Reserva actualizada con éxito", reservaActualizada.getId()));
        } catch (IllegalArgumentException e) {
            logger.error("Datos inválidos en la petición");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Respuesta(false, "Datos inválidos en la petición", null));
        }
    }
    // DELETE reservas/id
    @Operation(summary = "Eliminar una reserva", description = "Elimina una reserva existente dado su identificador")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Reserva eliminada con éxito",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": true, \"mensaje\": \"Reserva eliminada con éxito\", \"idGenerado\": null}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reserva no encontrada",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": false, \"mensaje\": \"Reserva no encontrada\", \"idGenerado\": null}"
                            )
                    )
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Respuesta> eliminarReserva(@PathVariable String id) {
        Reserva reserva = reservaService.findById(id);
        if (reserva == null) {
            logger.error("Reserva no encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Respuesta(false, "Reserva no encontrada", null));
        }

        reservaService.eliminarReserva(id);
        logger.info("Reserva eliminada con éxito");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Respuesta(true, "Reserva eliminada con éxito", id));
    }

    // GET reservas/dia/{numDia}
    @Operation(summary = "Obtener reservas por día", description = "Devuelve todas las reservas para un día específico.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reservas encontradas",
                    content = @Content(
                            schema = @Schema(
                                    example = "[ { \"sala\": \"La Casa del Terror\", \"dia\": 3, \"hora\": 18, \"contacto\": { \"titular\": \"Diego\", \"telefono\": 685414005 }, \"jugadores\": 4 } ]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "204",
                    description = "No hay reservas para ese día",
                    content = @Content(
                            schema = @Schema(
                                    example = "{\"exito\": false, \"mensaje\": \"Reserva no encontrada\", \"idGenerado\": null}"
                            )
                    )
            )
    })
    @GetMapping("/dia/{numDia}")
    public ResponseEntity<List<Reserva>> obtenerReservasPorDia(@PathVariable int numDia) {
        List<Reserva> reservas = reservaService.obtenerReservasPorDia(numDia);
        logger.info("Reservas encontradas");
        return reservas.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(reservas);
    }


}
