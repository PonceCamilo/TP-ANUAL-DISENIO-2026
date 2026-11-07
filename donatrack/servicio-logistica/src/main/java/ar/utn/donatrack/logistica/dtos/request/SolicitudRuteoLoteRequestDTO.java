package ar.utn.donatrack.logistica.dtos.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Payload que ProveedorRuteoExternoAdapter envía al proveedor/mock de ruteo
 * para un lote: todas las donaciones del lote y todos los camiones disponibles.
 * El proveedor decide qué donaciones lleva cada camión y en qué orden.
 */
@Getter
@Setter
@NoArgsConstructor
public class SolicitudRuteoLoteRequestDTO {
    private UUID loteId;
    private List<CamionParaRutearRequestDTO> camiones;
    private List<DonacionParaRutearRequestDTO> donaciones;
}
