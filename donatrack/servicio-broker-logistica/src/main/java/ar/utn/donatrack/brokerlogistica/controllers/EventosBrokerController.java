package ar.utn.donatrack.brokerlogistica.controllers;

import ar.utn.donatrack.brokerlogistica.dtos.EventoExternoDTO;
import ar.utn.donatrack.brokerlogistica.services.BrokerEventosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhooks de los proveedores. Cada uno tiene su endpoint porque cada uno
 * publica con su propio formato.
 */
@RestController
@RequestMapping("/api/broker/eventos")
@RequiredArgsConstructor
@Tag(name = "Eventos", description = "Eventos de entrega de los proveedores, reenviados a Donaciones")
public class EventosBrokerController {

    private final BrokerEventosService brokerEventosService;

    @Operation(
            summary = "Evento de servicio-logistica",
            description = "Recibe INICIO_RUTA, ENTREGA_CONFIRMADA o ENTREGA_NO_RECIBIDA (campo `tipo`) y lo reenvía al callback de Donaciones.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Evento reenviado a Donaciones"),
                    @ApiResponse(responseCode = "400", description = "Tipo de evento desconocido"),
                    @ApiResponse(responseCode = "502", description = "Donaciones no aceptó el evento")
            }
    )
    @PostMapping("/donatrack")
    public ResponseEntity<Void> eventoDonatrack(@RequestBody Map<String, Object> evento) {
        brokerEventosService.procesarEventoDonatrack(evento);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Evento de servicio-logistica-externa",
            description = "Recibe el cambio de estado de un envío (EN_CAMINO, ENTREGADO, FALLIDO), lo traduce y lo reenvía a Donaciones. "
                    + "ACEPTADO se ignora.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Evento procesado"),
                    @ApiResponse(responseCode = "400", description = "Evento inválido o estado desconocido"),
                    @ApiResponse(responseCode = "502", description = "Donaciones no aceptó el evento")
            }
    )
    @PostMapping("/externa")
    public ResponseEntity<Map<String, Boolean>> eventoExterno(@Valid @RequestBody EventoExternoDTO evento) {
        boolean reenviado = brokerEventosService.procesarEventoExterno(evento);
        return ResponseEntity.ok(Map.of("reenviado", reenviado));
    }
}
