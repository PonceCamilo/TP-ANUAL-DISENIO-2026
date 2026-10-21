package ar.utn.donatrack.logistica.dtos.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Respuesta síncrona del proveedor/mock a la planificación de un lote.
 * Trae una ruta por cada camión que efectivamente lleva donaciones.
 */
@Getter
@Setter
@NoArgsConstructor
public class PlanificacionProveedorResponseDTO {
    private List<RutaPlanificadaProveedorDTO> rutas;
}
