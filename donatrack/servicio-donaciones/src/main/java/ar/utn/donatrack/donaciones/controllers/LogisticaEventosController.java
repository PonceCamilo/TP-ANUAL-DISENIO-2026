package ar.utn.donatrack.donaciones.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import ar.utn.donatrack.donaciones.dtos.request.EntregaExitosaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.EntregaFallidaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.InicioRutaCallbackDTO;
import ar.utn.donatrack.donaciones.services.logistica.ManejadorEntregaExitosa;
import ar.utn.donatrack.donaciones.services.logistica.ManejadorEntregaFallida;
import ar.utn.donatrack.donaciones.services.logistica.ManejadorInicioDeRuta;
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
@Tag(name = "Eventos de logística", description = "Avisos que manda el broker de logística sobre cada entrega. Son "
    + "idempotentes: un aviso repetido responde 200 sin cambiar nada ni volver a notificar.")
public class LogisticaEventosController {

    private final ManejadorInicioDeRuta inicioDeRuta;
    private final ManejadorEntregaExitosa entregaExitosa;
    private final ManejadorEntregaFallida entregaFallida;

    @Operation(
            summary = "El chofer inició la ruta",
            description = "Las donaciones de la ruta pasan a EN_TRASLADO y se avisa a sus donantes y entidades con el "
                    + "link al mapa de seguimiento. Si una donación no puede pasar, no pasa ninguna.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Aviso procesado (o repetido, sin cambios)"),
                    @ApiResponse(responseCode = "400", description = "Aviso incompleto"),
                    @ApiResponse(responseCode = "404", description = "Alguna donación de la ruta no existe"),
                    @ApiResponse(responseCode = "422", description = "Alguna donación no está lista para entregar")
            }
    )
    @PostMapping("/inicio-ruta")
    public ResponseEntity<Void> recibirInicioRuta(
            @RequestBody @Valid InicioRutaCallbackDTO dto
    ) {
        inicioDeRuta.procesar(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "La entidad recibió la donación",
            description = "La donación pasa a ENTREGADA con la fecha que informa logística, se imputa a las necesidades "
                    + "de la entidad y se avisa a entidad, donante e incentivos con el comprobante.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Aviso procesado (o repetido, sin cambios)"),
                    @ApiResponse(responseCode = "400", description = "Aviso incompleto"),
                    @ApiResponse(responseCode = "404", description = "La donación no existe"),
                    @ApiResponse(responseCode = "422", description = "La donación no está en traslado")
            }
    )
    @PostMapping("/entrega-exitosa")
    public ResponseEntity<Void> recibirEntregaExitosa(
            @RequestBody @Valid EntregaExitosaCallbackDTO dto
    ) {
        entregaExitosa.procesar(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "La entrega no pudo concretarse",
            description = "La donación pasa a ENTREGA_FALLIDA con el motivo y se avisa a la entidad, a la persona "
                    + "donante y a la persona administradora. Si es replanificable, un nuevo inicio de ruta la "
                    + "vuelve a poner en traslado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Aviso procesado (o repetido, sin cambios)"),
                    @ApiResponse(responseCode = "400", description = "Aviso incompleto"),
                    @ApiResponse(responseCode = "404", description = "La donación no existe"),
                    @ApiResponse(responseCode = "422", description = "La donación no está en traslado")
            }
    )
    @PostMapping("/entrega-fallida")
    public ResponseEntity<Void> recibirEntregaFallida(
            @RequestBody @Valid EntregaFallidaCallbackDTO dto
    ) {
        entregaFallida.procesar(dto);
        return ResponseEntity.ok().build();
    }
}
