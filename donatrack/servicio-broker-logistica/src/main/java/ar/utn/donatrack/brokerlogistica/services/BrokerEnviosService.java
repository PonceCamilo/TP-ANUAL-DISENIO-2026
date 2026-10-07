package ar.utn.donatrack.brokerlogistica.services;

import ar.utn.donatrack.brokerlogistica.adapters.ProveedorLogisticaAdapter;
import ar.utn.donatrack.brokerlogistica.dtos.EnvioAsignadoDTO;
import ar.utn.donatrack.brokerlogistica.dtos.ProveedorEstadoDTO;
import ar.utn.donatrack.brokerlogistica.dtos.ResultadoEnvioResponse;
import ar.utn.donatrack.brokerlogistica.dtos.SolicitudEnvioRequest;
import ar.utn.donatrack.brokerlogistica.exceptions.EnvioNoRegistradoException;
import ar.utn.donatrack.brokerlogistica.exceptions.ProveedorNoDisponibleException;
import ar.utn.donatrack.brokerlogistica.exceptions.SinProveedorDisponibleException;
import ar.utn.donatrack.brokerlogistica.repositories.RegistroEnviosRepository;
import ar.utn.donatrack.brokerlogistica.seleccion.CriterioSeleccionProveedor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Ida del broker: Donaciones pide un envío y el broker se lo ofrece a los
 * proveedores candidatos (Strategy) en orden, salteando los que no están
 * disponibles, hasta que uno lo acepta (Adapter).
 */
@Service
public class BrokerEnviosService {

    private static final Logger log = LoggerFactory.getLogger(BrokerEnviosService.class);

    private final List<ProveedorLogisticaAdapter> proveedores;
    private final CriterioSeleccionProveedor criterio;
    private final RegistroEnviosRepository registro;

    public BrokerEnviosService(List<ProveedorLogisticaAdapter> proveedores,
                               CriterioSeleccionProveedor criterio,
                               RegistroEnviosRepository registro) {
        this.proveedores = proveedores;
        this.criterio = criterio;
        this.registro = registro;
    }

    public ResultadoEnvioResponse solicitarEnvio(SolicitudEnvioRequest solicitud) {
        List<String> descartados = new ArrayList<>();

        for (ProveedorLogisticaAdapter proveedor : criterio.candidatos(proveedores, solicitud.proveedor())) {
            if (!proveedor.disponible()) {
                descartados.add(proveedor.nombre());
                continue;
            }
            try {
                List<EnvioAsignadoDTO> envios = proveedor.solicitarEnvios(solicitud.donaciones());
                envios.forEach(registro::guardar);
                log.info("[Broker] {} donaciones asignadas a {} (descartados: {})",
                        envios.size(), proveedor.nombre(), descartados);
                return new ResultadoEnvioResponse(proveedor.nombre(), descartados, envios);
            } catch (ProveedorNoDisponibleException e) {
                log.warn("[Broker] {}: {}", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "sin detalle");
                descartados.add(proveedor.nombre());
            }
        }

        throw new SinProveedorDisponibleException(descartados);
    }

    public EnvioAsignadoDTO consultarEnvio(UUID idDonacion) {
        return registro.buscarPorDonacion(idDonacion)
                .orElseThrow(() -> new EnvioNoRegistradoException(idDonacion));
    }

    /** Proveedores en orden de prioridad, con su health check actual. */
    public List<ProveedorEstadoDTO> estadoProveedores() {
        List<ProveedorLogisticaAdapter> ordenados = criterio.candidatos(proveedores, null);
        return IntStream.range(0, ordenados.size())
                .mapToObj(i -> new ProveedorEstadoDTO(ordenados.get(i).nombre(), i + 1, ordenados.get(i).disponible()))
                .toList();
    }
}
