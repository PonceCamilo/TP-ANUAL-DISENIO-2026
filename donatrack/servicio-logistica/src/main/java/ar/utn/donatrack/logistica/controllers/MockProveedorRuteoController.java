package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.request.CallbackParadaDTO;
import ar.utn.donatrack.logistica.dtos.request.CamionParaRutearRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.DonacionParaRutearRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.SolicitudRuteoLoteRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.PlanificacionProveedorResponseDTO;
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
 * Recibe un lote completo: todas sus donaciones y todos los camiones
 * disponibles. Decide qué grupo lleva cada camión y responde, en el mismo
 * request/response, una ruta por camión usado. No calcula un recorrido
 * geográfico: no hay coordenadas en el pedido.
 *
 * Regla, en el orden de llegada de las donaciones:
 * 1. Un grupo por entidad beneficiaria. Las donaciones del grupo conservan
 *    el orden en que llegaron.
 * 2. Cada grupo entero va al camión que hasta ese momento tiene menos
 *    donaciones. Si empatan, gana el que aparece primero en la lista.
 * 3. Un camión sin donaciones no genera ruta.
 * 4. Las paradas de un camión quedan en el orden en que se le asignaron
 *    los grupos. La capacidad del camión viaja en el pedido y no se usa
 *    para cortar la carga: toda donación recibida queda en alguna ruta.
 */
@RestController
@RequestMapping("/ruteo")
@Tag(name = "Mock Proveedor Ruteo", description = "Stub del proveedor externo de ruteo para pruebas locales")
public class MockProveedorRuteoController {

    private static final Logger log = LoggerFactory.getLogger(MockProveedorRuteoController.class);

    @Operation(
            summary = "Planificar las rutas de un lote (mock)",
            description = "Simula al proveedor externo: agrupa las donaciones por entidad beneficiaria, "
                    + "asigna cada grupo entero al camión con menos donaciones y devuelve una ruta por camión usado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Rutas planificadas del lote",
                            content = @Content(schema = @Schema(implementation = PlanificacionProveedorResponseDTO.class)))
            }
    )
    @PostMapping("/planificar")
    public ResponseEntity<PlanificacionProveedorResponseDTO> planificar(@RequestBody SolicitudRuteoLoteRequestDTO dto) {
        List<CamionParaRutearRequestDTO> camiones = dto.getCamiones() == null ? List.of() : dto.getCamiones();
        List<DonacionParaRutearRequestDTO> donaciones = dto.getDonaciones() == null ? List.of() : dto.getDonaciones();
        log.info("[MockProveedorRuteoController] Planificando lote={}, {} camiones, {} donaciones",
                dto.getLoteId(), camiones.size(), donaciones.size());

        PlanificacionProveedorResponseDTO respuesta = new PlanificacionProveedorResponseDTO();
        respuesta.setRutas(repartir(camiones, donaciones));
        return ResponseEntity.ok(respuesta);
    }

    private List<RutaPlanificadaProveedorDTO> repartir(
            List<CamionParaRutearRequestDTO> camiones,
            List<DonacionParaRutearRequestDTO> donaciones) {
        if (camiones.isEmpty() || donaciones.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<DonacionParaRutearRequestDTO>> grupos = new LinkedHashMap<>();
        for (DonacionParaRutearRequestDTO donacion : donaciones) {
            grupos.computeIfAbsent(donacion.getIdEntidadBeneficiaria(), id -> new ArrayList<>()).add(donacion);
        }

        Map<UUID, List<List<DonacionParaRutearRequestDTO>>> gruposPorCamion = new LinkedHashMap<>();
        Map<UUID, Integer> cantidadPorCamion = new LinkedHashMap<>();
        for (CamionParaRutearRequestDTO camion : camiones) {
            gruposPorCamion.put(camion.getId(), new ArrayList<>());
            cantidadPorCamion.put(camion.getId(), 0);
        }

        for (List<DonacionParaRutearRequestDTO> grupo : grupos.values()) {
            CamionParaRutearRequestDTO elegido = camiones.getFirst();
            int menorCantidad = Integer.MAX_VALUE;
            for (CamionParaRutearRequestDTO camion : camiones) {
                int cantidad = cantidadPorCamion.get(camion.getId());
                if (cantidad < menorCantidad) {
                    menorCantidad = cantidad;
                    elegido = camion;
                }
            }
            gruposPorCamion.get(elegido.getId()).add(grupo);
            cantidadPorCamion.put(elegido.getId(), menorCantidad + grupo.size());
        }

        List<RutaPlanificadaProveedorDTO> rutas = new ArrayList<>();
        for (CamionParaRutearRequestDTO camion : camiones) {
            List<List<DonacionParaRutearRequestDTO>> gruposDelCamion = gruposPorCamion.get(camion.getId());
            if (gruposDelCamion.isEmpty()) {
                continue;
            }
            RutaPlanificadaProveedorDTO ruta = new RutaPlanificadaProveedorDTO();
            ruta.setCamionId(camion.getId());
            ruta.setParadas(aParadas(gruposDelCamion));
            rutas.add(ruta);
        }
        return rutas;
    }

    private List<CallbackParadaDTO> aParadas(List<List<DonacionParaRutearRequestDTO>> grupos) {
        List<CallbackParadaDTO> paradas = new ArrayList<>();
        int orden = 1;
        for (List<DonacionParaRutearRequestDTO> grupo : grupos) {
            DonacionParaRutearRequestDTO primera = grupo.getFirst();
            CallbackParadaDTO parada = new CallbackParadaDTO();
            parada.setOrden(orden++);
            parada.setIdEntidadBeneficiaria(primera.getIdEntidadBeneficiaria());
            parada.setDireccion(primera.getDireccionEntrega());
            parada.setDonacionesIds(grupo.stream().map(DonacionParaRutearRequestDTO::getIdDonacion).toList());
            paradas.add(parada);
        }
        return paradas;
    }
}
