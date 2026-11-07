package ar.utn.donatrack.brokerlogistica.controllers;

import ar.utn.donatrack.brokerlogistica.dtos.EnvioAsignadoDTO;
import ar.utn.donatrack.brokerlogistica.dtos.ProveedorEstadoDTO;
import ar.utn.donatrack.brokerlogistica.dtos.ResultadoEnvioResponse;
import ar.utn.donatrack.brokerlogistica.dtos.SolicitudEnvioRequest;
import ar.utn.donatrack.brokerlogistica.services.BrokerEnviosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/broker")
@RequiredArgsConstructor
@Tag(name = "Envíos", description = "Pedidos de envío de Donaciones hacia los proveedores de logística")
public class EnviosBrokerController {

    private final BrokerEnviosService brokerEnviosService;

    @Operation(
            summary = "Solicitar envío",
            description = "Donaciones entrega donaciones en ASIGNACION_REALIZADA. Si no se indica `proveedor`, el broker "
                    + "las ofrece según la prioridad configurada y hace fallback al siguiente proveedor si uno no está disponible.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Un proveedor tomó las donaciones"),
                    @ApiResponse(responseCode = "400", description = "Request inválido o proveedor desconocido"),
                    @ApiResponse(responseCode = "503", description = "Ningún proveedor disponible")
            }
    )
    @PostMapping("/envios")
    public ResponseEntity<ResultadoEnvioResponse> solicitarEnvio(@Valid @RequestBody SolicitudEnvioRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brokerEnviosService.solicitarEnvio(solicitud));
    }

    @Operation(
            summary = "Consultar envío de una donación",
            description = "Qué proveedor tomó la donación y con qué id de seguimiento.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Envío encontrado"),
                    @ApiResponse(responseCode = "404", description = "La donación no se envió por el broker")
            }
    )
    @GetMapping("/envios/{idDonacion}")
    public ResponseEntity<EnvioAsignadoDTO> consultarEnvio(@PathVariable UUID idDonacion) {
        return ResponseEntity.ok(brokerEnviosService.consultarEnvio(idDonacion));
    }

    @Operation(summary = "Proveedores", description = "Proveedores registrados, en orden de prioridad, con su disponibilidad actual.")
    @GetMapping("/proveedores")
    public ResponseEntity<List<ProveedorEstadoDTO>> proveedores() {
        return ResponseEntity.ok(brokerEnviosService.estadoProveedores());
    }
}
