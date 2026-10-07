package ar.utn.donatrack.logistica.dtos.response;

import ar.utn.donatrack.logistica.dtos.request.CallbackParadaDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Respuesta síncrona del proveedor/mock de ruteo a la solicitud de
 * planificación de UN camión: la ruta (paradas y donaciones asignadas)
 * para ese camión (ver ProveedorRuteoExternoAdapter y
 * MockProveedorRuteoController).
 */
@Getter
@Setter
@NoArgsConstructor
public class RutaPlanificadaProveedorDTO {
    private UUID camionId;
    private List<CallbackParadaDTO> paradas;
}
