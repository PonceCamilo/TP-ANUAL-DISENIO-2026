package com.donatrack.donaciones.controlador;

import com.donatrack.donaciones.dto.request.RegistroPersonaHumanaRequest;
import com.donatrack.donaciones.dto.request.RegistroPersonaJuridicaRequest;
import com.donatrack.donaciones.dto.response.PersonaDonanteResponse;
import com.donatrack.donaciones.servicio.PersonaDonanteServicio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Expone la gestión de personas donantes como API REST.
 *
 * Endpoints:
 *  POST   /api/donantes/humanos          → registrar persona humana
 *  POST   /api/donantes/juridicos        → registrar persona jurídica
 *  GET    /api/donantes                  → listar personas donantes activas
 *  GET    /api/donantes/todos            → listar todas (incluye dadas de baja)
 *  GET    /api/donantes/{id}             → obtener por ID
 *  PUT    /api/donantes/humanos/{id}     → actualizar persona humana
 *  PUT    /api/donantes/juridicos/{id}   → actualizar persona jurídica
 *  DELETE /api/donantes/{id}             → dar de baja (baja lógica)
 */
@RestController
@RequestMapping("/api/donantes")
public class PersonaDonanteControlador {

    private final PersonaDonanteServicio servicio;

    public PersonaDonanteControlador(PersonaDonanteServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/humanos")
    public ResponseEntity<PersonaDonanteResponse> registrarPersonaHumana(
            @Valid @RequestBody RegistroPersonaHumanaRequest request) {
        PersonaDonanteResponse response = servicio.registrarPersonaHumana(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/juridicos")
    public ResponseEntity<PersonaDonanteResponse> registrarPersonaJuridica(
            @Valid @RequestBody RegistroPersonaJuridicaRequest request) {
        PersonaDonanteResponse response = servicio.registrarPersonaJuridica(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PersonaDonanteResponse>> listarActivos() {
        return ResponseEntity.ok(servicio.listarActivos());
    }

    @GetMapping("/todos")
    public ResponseEntity<List<PersonaDonanteResponse>> listarTodos() {
        return ResponseEntity.ok(servicio.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonaDonanteResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(servicio.obtenerPorId(id));
    }

    @PutMapping("/humanos/{id}")
    public ResponseEntity<PersonaDonanteResponse> actualizarPersonaHumana(
            @PathVariable UUID id,
            @Valid @RequestBody RegistroPersonaHumanaRequest request) {
        return ResponseEntity.ok(servicio.actualizarPersonaHumana(id, request));
    }

    @PutMapping("/juridicos/{id}")
    public ResponseEntity<PersonaDonanteResponse> actualizarPersonaJuridica(
            @PathVariable UUID id,
            @Valid @RequestBody RegistroPersonaJuridicaRequest request) {
        return ResponseEntity.ok(servicio.actualizarPersonaJuridica(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable UUID id) {
        servicio.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}
