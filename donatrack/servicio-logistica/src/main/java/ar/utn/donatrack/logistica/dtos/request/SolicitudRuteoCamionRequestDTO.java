package ar.utn.donatrack.logistica.dtos.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Payload que ProveedorRuteoExternoAdapter envía al proveedor/mock de ruteo
 * para pedir la planificación de UN camión: el lote al que pertenece, ese
 * camión y las donaciones que le tocan a él.
 */
@Getter
@Setter
@NoArgsConstructor
public class SolicitudRuteoCamionRequestDTO {
    private UUID loteId;
    private CamionParaRutearRequestDTO camion;
    private List<DonacionParaRutearRequestDTO> donaciones;
}
