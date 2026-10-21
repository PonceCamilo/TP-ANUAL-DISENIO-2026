package ar.utn.donatrack.logistica.dtos.response;

import ar.utn.donatrack.logistica.dtos.request.CallbackParadaDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Una ruta dentro de la respuesta del proveedor/mock: el camión y las
 * paradas, con las donaciones de cada una. El cuerpo completo del lote
 * es PlanificacionProveedorResponseDTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class RutaPlanificadaProveedorDTO {
    private UUID camionId;
    private List<CallbackParadaDTO> paradas;
}
