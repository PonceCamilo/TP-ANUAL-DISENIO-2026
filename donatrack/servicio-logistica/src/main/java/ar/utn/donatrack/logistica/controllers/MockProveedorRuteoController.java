package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.request.CallbackParadaDTO;
import ar.utn.donatrack.logistica.dtos.request.DonacionParaRutearRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.SolicitudRuteoCamionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaPlanificadaProveedorDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mock del proveedor externo de ruteo. Reemplaza al servicio real
 * (que normalmente viviria en otro host) para poder probar en local el flujo
 * de planificacion sin un 'Connection Refused'.
 *
 * Recibe la solicitud de UN camión (loteId, camión, donaciones que le tocan
 * a ese camión, ver ProveedorRuteoExternoAdapter) y responde, en el mismo
 * request/response, la ruta planificada para ese camión: agrupa las
 * donaciones recibidas por entidad beneficiaria y arma una parada por
 * entidad, en el orden en que llegaron. No es un algoritmo de ruteo real,
 * solo genera una ruta determinística para poder probar el flujo end to end.
 */
@RestController
@RequestMapping("/ruteo")
@Tag(name = "Mock Proveedor Ruteo", description = "Stub del proveedor externo de ruteo para pruebas locales")
public class MockProveedorRuteoController {

    private static final Logger log = LoggerFactory.getLogger(MockProveedorRuteoController.class);

    @Operation(
            summary = "Planificar la ruta de un camión (mock)",
            description = "Simula al proveedor externo: agrupa las donaciones recibidas por entidad beneficiaria "
                    + "y devuelve, en el mismo response, la ruta planificada para el camión indicado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ruta planificada para el camión",
                            content = @Content(schema = @Schema(implementation = RutaPlanificadaProveedorDTO.class)))
            }
    )
    @PostMapping("/planificar")
    public ResponseEntity<RutaPlanificadaProveedorDTO> planificar(@RequestBody SolicitudRuteoCamionRequestDTO dto) {
        log.info("[MockProveedorRuteoController] Planificando lote={}, camion={}, {} donaciones",
                dto.getLoteId(), dto.getCamion().getId(), dto.getDonaciones().size());

        RutaPlanificadaProveedorDTO ruta = new RutaPlanificadaProveedorDTO();
        ruta.setCamionId(dto.getCamion().getId());
        ruta.setParadas(agruparPorEntidad(dto.getDonaciones()));
        return ResponseEntity.ok(ruta);
    }

    private List<CallbackParadaDTO> agruparPorEntidad(List<DonacionParaRutearRequestDTO> donaciones) {
        Map<UUID, CallbackParadaDTO> paradasPorEntidad = new LinkedHashMap<>();
        for (DonacionParaRutearRequestDTO donacion : donaciones) {
            CallbackParadaDTO parada = paradasPorEntidad.computeIfAbsent(donacion.getIdEntidadBeneficiaria(), idEntidad -> {
                CallbackParadaDTO nueva = new CallbackParadaDTO();
                nueva.setOrden(paradasPorEntidad.size() + 1);
                nueva.setIdEntidadBeneficiaria(idEntidad);
                nueva.setDireccion(donacion.getDireccionEntrega());
                nueva.setDonacionesIds(new ArrayList<>());
                return nueva;
            });
            parada.getDonacionesIds().add(donacion.getIdDonacion());
        }
        return new ArrayList<>(paradasPorEntidad.values());
    }
}
