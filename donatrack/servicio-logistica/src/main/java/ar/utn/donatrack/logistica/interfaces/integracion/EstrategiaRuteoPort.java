package ar.utn.donatrack.logistica.interfaces.integracion;

import ar.utn.donatrack.logistica.dtos.response.RutaPlanificadaProveedorDTO;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.planificacion.DonacionLote;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;

import java.util.List;

/**
 * Strategy: encapsula el algoritmo/proveedor que calcula las rutas.
 * Hoy hay una sola implementación (proveedor externo vía REST), pero el
 * puerto permite cambiarla sin tocar PlanificacionRutasService.
 *
 * La planificación se pide una vez por lote: se le pasan todas las
 * donaciones del lote y todos los camiones disponibles. El proveedor
 * decide la carga de cada camión y responde, en el mismo request/response
 * HTTP, una ruta por camión usado.
 */
public interface EstrategiaRuteoPort {
    List<RutaPlanificadaProveedorDTO> planificar(LotePlanificacion lote, List<Camion> camiones, List<DonacionLote> donaciones);
}
