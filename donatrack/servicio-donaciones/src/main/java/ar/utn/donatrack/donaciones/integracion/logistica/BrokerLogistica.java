package ar.utn.donatrack.donaciones.integracion.logistica;

import ar.utn.donatrack.donaciones.interfaces.integracion.LogisticaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Broker de integración con logística (Entrega 4).
 *
 * El enunciado pide que "deberá permitir seleccionar entre más de 1 servicio de
 * logística disponible, entendiendo que está el propio servicio construido y
 * otro servicio potencial que cumple igual objetivo".
 *
 * CRITERIO DE SELECCIÓN ELEGIDO: preferencia configurable + failover automático.
 *
 *   1. Los proveedores se ordenan poniendo primero al configurado como preferido
 *      (`logistica.proveedor.preferido`, por defecto "propia").
 *   2. Se recorre esa lista y se le ofrece el trabajo al primero que responda
 *      que está disponible.
 *   3. Si ese proveedor termina rechazando la solicitud, se sigue con el
 *      siguiente en lugar de dar la planificación por perdida.
 *
 * Por qué este criterio y no otro: la ONG tiene una preferencia operativa clara
 * (usar su propia flota, que no le cuesta plata) pero no puede quedarse sin
 * repartir si esa flota no está disponible. Un criterio por costo o por volumen
 * exigiría datos que el sistema hoy no tiene; uno puramente aleatorio o por
 * round-robin repartiría trabajo a un tercero aun teniendo camiones ociosos.
 *
 * Quien lo usa (PlanificacionEntregasService) no conoce a los proveedores: pide
 * una planificación y el broker resuelve a quién derivarla.
 */
@Component
public class BrokerLogistica {

    private static final Logger log = LoggerFactory.getLogger(BrokerLogistica.class);

    private final List<LogisticaPort> proveedores;
    private final String proveedorPreferido;

    /**
     * Spring inyecta acá TODOS los beans que implementen LogisticaPort. Sumar un
     * proveedor nuevo es crear otro adapter: este constructor no cambia.
     */
    public BrokerLogistica(List<LogisticaPort> proveedores,
                           @Value("${logistica.proveedor.preferido:propia}") String proveedorPreferido) {
        this.proveedores = proveedores;
        this.proveedorPreferido = proveedorPreferido;
    }

    public ResultadoPlanificacion planificar(List<SolicitudEntrega> entregas) {
        if (entregas.isEmpty()) {
            return ResultadoPlanificacion.rechazada("ninguno", "No había donaciones para planificar.");
        }
        if (proveedores.isEmpty()) {
            log.error("[Broker] No hay ningún proveedor de logística registrado.");
            return ResultadoPlanificacion.sinProveedorDisponible();
        }

        for (LogisticaPort proveedor : ordenadosPorPreferencia()) {
            if (!proveedor.estaDisponible()) {
                log.warn("[Broker] El proveedor '{}' no está disponible, se intenta con el siguiente.",
                        proveedor.nombre());
                continue;
            }

            log.info("[Broker] Derivando {} donaciones al proveedor '{}'.", entregas.size(), proveedor.nombre());
            ResultadoPlanificacion resultado = proveedor.solicitarPlanificacion(entregas);

            if (resultado.aceptada()) {
                return resultado;
            }

            // Estaba disponible pero rechazó el trabajo: se sigue con el próximo.
            log.warn("[Broker] El proveedor '{}' rechazó la solicitud: {}",
                    proveedor.nombre(), resultado.detalle());
        }

        log.error("[Broker] Ningún proveedor pudo hacerse cargo de las {} donaciones.", entregas.size());
        return ResultadoPlanificacion.sinProveedorDisponible();
    }

    /** El proveedor preferido primero; los demás quedan como alternativas, en orden estable. */
    private List<LogisticaPort> ordenadosPorPreferencia() {
        return proveedores.stream()
                .sorted(Comparator.comparing(proveedor -> !esPreferido(proveedor)))
                .toList();
    }

    private boolean esPreferido(LogisticaPort proveedor) {
        return proveedor.nombre().equalsIgnoreCase(proveedorPreferido);
    }

    /** Nombres de los proveedores registrados, para diagnóstico y para los tests. */
    public List<String> proveedoresRegistrados() {
        return proveedores.stream().map(LogisticaPort::nombre).toList();
    }
}
