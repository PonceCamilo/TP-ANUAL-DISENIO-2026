package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.dtos.request.EntregaExitosaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.EntregaFallidaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.InicioRutaCallbackDTO;
import ar.utn.donatrack.donaciones.services.LogisticaEventosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone los endpoints de callback que el Servicio de Logística utiliza
 * para notificar eventos relevantes al Servicio de Donaciones.
 *
 * De esta forma se respeta la restricción de Entrega 3: Logística no llama
 * directamente a Notificaciones; es Donaciones quien orquesta ese flujo.
 *
 * Endpoints:
 *   POST /logistica/eventos/inicio-ruta    → chofer inicia el recorrido
 *   POST /logistica/eventos/entrega-exitosa → entidad confirma recepción
 *   POST /logistica/eventos/entrega-fallida → entrega no concretada
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/logistica/eventos")
@Validated
public class LogisticaEventosController {

    private final LogisticaEventosService logisticaEventosService;

    @PostMapping("/inicio-ruta")
    public ResponseEntity<Void> recibirInicioRuta(
            @RequestBody @Valid InicioRutaCallbackDTO dto
    ) {
        logisticaEventosService.procesarInicioRuta(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/entrega-exitosa")
    public ResponseEntity<Void> recibirEntregaExitosa(
            @RequestBody @Valid EntregaExitosaCallbackDTO dto
    ) {
        logisticaEventosService.procesarEntregaExitosa(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/entrega-fallida")
    public ResponseEntity<Void> recibirEntregaFallida(
            @RequestBody @Valid EntregaFallidaCallbackDTO dto
    ) {
        logisticaEventosService.procesarEntregaFallida(dto);
        return ResponseEntity.ok().build();
    }
}
