package ar.utn.donatrack.donaciones.interfaces.integracion;

import ar.utn.donatrack.donaciones.integracion.logistica.ResultadoPlanificacion;
import ar.utn.donatrack.donaciones.integracion.logistica.SolicitudEntrega;

import java.util.List;

/**
 * Puerto de salida hacia un proveedor de logística.
 *
 * La Entrega 4 pide un broker que "permita seleccionar entre más de 1 servicio
 * de logística disponible, entendiendo que está el propio servicio construido y
 * otro servicio potencial que cumple igual objetivo". Este puerto es lo que hace
 * intercambiables a esos proveedores: cada uno se implementa como un adapter y
 * el BrokerLogistica elige entre todos los que estén registrados.
 *
 * Sumar un proveedor nuevo es crear otra implementación de esta interfaz; ni el
 * broker ni el servicio que dispara la planificación cambian.
 */
public interface LogisticaPort {

    /** Nombre con el que este proveedor se identifica en la configuración y los logs. */
    String nombre();

    /**
     * Sondeo liviano para saber si el proveedor está en condiciones de recibir
     * trabajo. El broker lo usa para elegir y para hacer failover.
     */
    boolean estaDisponible();

    /**
     * Deriva las donaciones al proveedor para que arme las rutas del día siguiente.
     * Nunca lanza: los problemas se informan en el ResultadoPlanificacion.
     */
    ResultadoPlanificacion solicitarPlanificacion(List<SolicitudEntrega> entregas);
}
