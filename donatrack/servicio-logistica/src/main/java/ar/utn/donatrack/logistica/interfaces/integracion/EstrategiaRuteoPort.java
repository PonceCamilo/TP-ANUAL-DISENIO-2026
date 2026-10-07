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
 * La planificación se pide de a un camión por vez: se le pasan las
 * donaciones que le corresponden a ese camión y el proveedor responde,
 * en el mismo request/response HTTP, la ruta planificada para él.
 */
public interface EstrategiaRuteoPort {
    RutaPlanificadaProveedorDTO planificarParaCamion(LotePlanificacion lote, Camion camion, List<DonacionLote> donaciones);
}
