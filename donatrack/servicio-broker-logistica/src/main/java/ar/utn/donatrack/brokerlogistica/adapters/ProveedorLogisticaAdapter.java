package ar.utn.donatrack.brokerlogistica.adapters;

import ar.utn.donatrack.brokerlogistica.dtos.DonacionEnvioDTO;
import ar.utn.donatrack.brokerlogistica.dtos.EnvioAsignadoDTO;

import java.util.List;

/**
 * Adapter: interfaz común que el broker usa para hablar con cualquier
 * proveedor de logística. Cada implementación traduce el contrato neutral del
 * broker al contrato propio de su proveedor. Sumar un proveedor nuevo es
 * agregar una implementación (@Component) y su nombre a la prioridad.
 */
public interface ProveedorLogisticaAdapter {

    /** Identificador del proveedor (ej. "DONATRACK", "EXTERNA"). */
    String nombre();

    /** Health check: si es false, el broker ni siquiera le manda el pedido. */
    boolean disponible();

    /**
     * Le pide al proveedor que se haga cargo de las donaciones.
     *
     * @throws ar.utn.donatrack.brokerlogistica.exceptions.ProveedorNoDisponibleException
     *         si el proveedor no responde o rechaza el pedido.
     */
    List<EnvioAsignadoDTO> solicitarEnvios(List<DonacionEnvioDTO> donaciones);
}
