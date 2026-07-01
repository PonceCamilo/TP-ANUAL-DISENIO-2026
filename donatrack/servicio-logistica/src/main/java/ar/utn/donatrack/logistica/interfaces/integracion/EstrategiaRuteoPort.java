package ar.utn.donatrack.logistica.interfaces.integracion;

import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;

import java.util.List;

/**
 * Strategy: encapsula el algoritmo/proveedor que calcula las rutas.
 * Hoy hay una sola implementación (proveedor externo vía REST), pero el
 * puerto permite cambiarla sin tocar PlanificacionRutasService.
 */
public interface EstrategiaRuteoPort {
    void solicitarPlanificacion(LotePlanificacion lote, List<Camion> camiones);
}
