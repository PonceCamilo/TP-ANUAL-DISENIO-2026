package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.services.AsignacionDonacionesService;
import ar.utn.donatrack.donaciones.services.DonacionService;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Expone el proceso de matchmaking de donaciones a entidades beneficiarias.
 * GET /ranking: ejecuta los algoritmos y propone candidatas.
 * POST /asignar: la persona administradora confirma el destino final.
 */
@RestController
@RequestMapping("/donaciones")
@RequiredArgsConstructor
public class AsignacionController {

    private final AsignacionDonacionesService asignacionService;
    private final DonacionService donacionService;
    private final DonacionesRepositoryInterface donacionesRepository;

    @GetMapping("/{id}/ranking")
    public ResponseEntity<AsignacionDonacionesService.ResultadoMatchmaking> obtenerRanking(@PathVariable UUID id) {
        Donacion donacion = donacionesRepository.obtenerPorId(id);
        if (donacion == null) {
            throw new DonacionNoEncontradaException(id);
        }
        return ResponseEntity.ok(asignacionService.generarRanking(donacion));
    }

    @PostMapping("/{id}/asignar")
    public ResponseEntity<Void> asignarEntidad(@PathVariable UUID id, @RequestParam UUID idEntidad) {
        donacionService.asignarEntidad(id, idEntidad);
        return ResponseEntity.ok().build();
    }
}