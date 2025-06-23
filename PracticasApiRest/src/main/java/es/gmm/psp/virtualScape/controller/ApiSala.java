package es.gmm.psp.virtualScape.controller;

import es.gmm.psp.virtualScape.model.Respuesta;
import es.gmm.psp.virtualScape.model.RespuestaEspecial;
import es.gmm.psp.virtualScape.model.Sala;
import es.gmm.psp.virtualScape.service.SalaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/virtual-escape/salas")
public class ApiSala {
    @Autowired
    private SalaService salaService;

    private static final Logger logger = LoggerFactory.getLogger(ApiSala.class);

    // POST /salas: Insetar una nueva sala
    @Operation(summary = "Añadir una nueva sala", description = "Crea una nueva sala. El id se genera automáticamente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sala creada correctamente",
                    content = @Content(schema = @Schema(example = "{\"exito\": true, \"mensaje\": \"Sala creada con éxito\", \"idGenerado\": \"123456789\"}"))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(example = "{\"exito\": false, \"mensaje\": \"Datos inválidos en la petición\", \"idGenerado\": null}"))),
    })
    @PostMapping
    public ResponseEntity<Respuesta> crearSala(@RequestParam String nombre,
                                               @RequestParam int capacidadMin,
                                               @RequestParam int capacidadMax,
                                               @RequestParam List<String> tematicas,
                                               @Valid @RequestBody Sala sala) {

        if (capacidadMin < 1 || capacidadMax > 8) {
            logger.error("El número de jugadores debe ser entre 1 y 8");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Respuesta(false, "El número de jugadores debe ser entre 1 y 8", null));
        }

        int totalJugadores = salaService.getTotalJugadores();
        if (totalJugadores + capacidadMax > 30) {
            logger.error("El número total de jugadores no puede superar los 30");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Respuesta(false, "El número total de jugadores no puede superar los 30", null));
        }

        sala.setId(null);
        sala.setNombre(nombre);
        sala.setCapacidadMin(capacidadMin);
        sala.setCapacidadMax(capacidadMax);
        sala.setTematicas(tematicas);

        Sala salaExistente = salaService.getByNombre(sala.getNombre());
        if (salaExistente != null) {
            logger.error("Ya existe una sala con ese nombre");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Respuesta(false, "Ya existe una sala con ese nombre", null));
        }

        Sala nuevaSala = salaService.crearSala(sala);
        Sala verificacion = salaService.findById(nuevaSala.getId());
        if (verificacion == null) {
            logger.error("Error al verificar la creación de la sala");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Respuesta(false, "Error al verificar la creación de la sala", null));
        }

        logger.info("Sala creada con éxito");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Respuesta(true, "Sala creada con éxito", nuevaSala.getId()));
    }

    // GET /salas: Listar todas las salas
    @Operation(summary = "Listar todas las salas", description = "Devuelve una lista de todas las salas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de salas encontrada",
                    content = @Content(array = @ArraySchema(schema = @Schema(
                            example = "[{\"sala\": \"El Bosque Maldito \", \"dia\": 3, \"hora\": 20, \"contacto\": { \"titular\": \"Manuela\", \"telefono\": 69248105 }, \"jugadores\": 6 }]")))),
            @ApiResponse(responseCode = "204", description = "No hay salas registradas",
                    content = @Content(schema = @Schema(
                            example = "{\"exito\": false, \"mensaje\": \"No hay salas registradas\", \"idGenerado\": null}")))
    })
    @GetMapping
    public ResponseEntity<?> getSalas() {
        List<Sala> salas = salaService.findAll();
        if (salas == null || salas.isEmpty()) {
            logger.error("No hay salas registradas");
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new Respuesta(false, "No hay salas registradas", null));
        }
        return ResponseEntity.ok(salas);
    }

    // GET /salas/{id}: Ver detalles de una sala específica
    @Operation(summary = "Obtener detalles de una sala", description = "Devuelve los detalles de una sala específica dado su identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sala encontrada",
                    content = @Content(schema = @Schema(example = "{\"sala\": \"El Bosque Maldito \", \"dia\": 3, \"hora\": 20, \"contacto\": { \"titular\": \"Manuela\", \"telefono\": 69248105 }, \"jugadores\": 6 }"))),
            @ApiResponse(responseCode = "404", description = "Sala no encontrada",
                    content = @Content(schema = @Schema(example = "{\"exito\": false, \"mensaje\": \"Sala no encontrada\", \"idGenerado\": null}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getSalaById(@PathVariable String id) {
        Sala sala = salaService.findById(id);
        if (sala == null) {
            logger.error("Sala no encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Respuesta(false, "Sala no encontrada", null));
        }
        logger.info("Sala encontrada");
        return ResponseEntity.ok(sala);
    }

    // PUT /salas/{id}: Actualizar una sala existente
    @Operation(summary = "Modificar una sala", description = "Actualiza por completo una sala existente con los nuevos datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sala actualizada correctamente",
                    content = @Content(schema = @Schema(example = "{\"exito\": true, \"mensaje\": \"Sala actualizada con éxito\", \"idGenerado\": 1234567890}"))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos en la petición",
                    content = @Content(schema = @Schema(example = "{\"exito\": false, \"mensaje\": \"Datos inválidos en la petición\", \"idGenerado\": null}"))),
            @ApiResponse(responseCode = "404", description = "Sala no encontrada",
                    content = @Content(schema = @Schema(example = "{\"exito\": false, \"mensaje\": \"Sala no encontrada\", \"idGenerado\": null}")))
    })
    @PutMapping("/{id}")
    public ResponseEntity<Respuesta> actualizarSala(@PathVariable String id,
                                                    @RequestParam String nombre,
                                                    @RequestParam int capacidadMin,
                                                    @RequestParam int capacidadMax,
                                                    @RequestParam List<String> tematicas,
                                                    @Valid @RequestBody Sala sala) {

        Sala salaExistente = salaService.findById(id);
        if (salaExistente == null) {
            logger.error("Sala no encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Respuesta(false, "Sala no encontrada", null));
        }

        if (!salaExistente.getNombre().equals(nombre)) {
            Sala salaConMismoNombre = salaService.getByNombre(nombre);
            if (salaConMismoNombre != null) {
                logger.error("El nombre de la sala ya esta en uso");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new Respuesta(false, "El nombre de la sala ya está en uso", null));
            }
        }

        int totalJugadoresAntes = salaService.getTotalJugadores() - salaExistente.getCapacidadMax();
        if (totalJugadoresAntes + capacidadMax > 30) {
            logger.error("El número total de jugadores no puede superar los 30");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Respuesta(false, "El número total de jugadores no puede superar los 30", null));
        }

        salaExistente.setNombre(nombre);
        salaExistente.setCapacidadMin(capacidadMin);
        salaExistente.setCapacidadMax(capacidadMax);
        salaExistente.setTematicas(tematicas);

        try {
            Sala salaActualizada = salaService.actualizarSala(salaExistente);
            Sala verificacion = salaService.findById(salaActualizada.getId());
            if (verificacion == null) {
                logger.error("Error al verificar la actualización");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new Respuesta(false, "Error al verificar la actualización", null));
            }
            logger.info("Sala actualizada con éxito");
            return ResponseEntity.ok(new Respuesta(true, "Sala actualizada con éxito", salaActualizada.getId()));
        } catch (IllegalArgumentException e) {
            logger.error("Datos inválidos en la petición");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Respuesta(false, "Datos inválidos en la petición", null));
        }
    }

    @Operation(summary = "Consultar salas por temática", description = "Consulta las salas disponibles que coincidan con la temática proporcionada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Salas encontradas",
                    content = @Content(array = @ArraySchema(schema =
                    @Schema(example = "[{\"id\": \"1\", \"nombre\": \"Sala A\", \"capacidadMin\": 4, \"capacidadMax\": 8, \"tematicas\": [\"Aventura\", \"Misterio\"]}, {\"id\": \"2\", \"nombre\": \"Sala B\", \"capacidadMin\": 2, \"capacidadMax\": 4, \"tematicas\": [\"Aventura\", \"Ciencia Ficción\"]}]")))),
            @ApiResponse(responseCode = "204", description = "No se encontraron salas con esa temática",
                    content = @Content(schema = @Schema(example = "{\"exito\": false, \"mensaje\": \"No se encontraron salas con esa temática\", \"idGenerado\": null}")))
    })
    @GetMapping("/tematica/{nombreTematica}")
    public ResponseEntity<?> getSalasPorTematica(@PathVariable String nombreTematica) {
        List<Sala> salas = salaService.findByTematica(nombreTematica);
        if (salas == null || salas.isEmpty()) {
            logger.error("No se encontraron salas con esa temática");
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        logger.info("Salas encontradas");
        return ResponseEntity.ok(salas);
    }

    @Operation(summary = "Listar salas con más reservas", description = "Devuelve las salas con mayor cantidad de reservas realizadas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos encontrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(example = "[{\"id\": \"1\", \"nombre\": \"Sala A\", \"capacidad\": 5}, {\"id\": \"2\", \"nombre\": \"Sala B\", \"capacidad\": 3}]")))),
            @ApiResponse(responseCode = "204", description = "No se encontraron datos", content = @Content(schema = @Schema(example = "{\"exito\": false, \"mensaje\": \"No se encontraron datos\", \"idGenerado\": null}")))
    })
    @GetMapping("/mas-reservadas")
    public ResponseEntity<List<RespuestaEspecial>> getSalasMasReservadas() {
        List<RespuestaEspecial> resultado = salaService.obtenerSalasMasReservadas();
        if (resultado == null || resultado.isEmpty()) {
            logger.error("No se encontraron datos");
            return ResponseEntity.noContent().build();
        }
        logger.info("Datos encontrados");
        return ResponseEntity.ok(resultado);
    }

}

