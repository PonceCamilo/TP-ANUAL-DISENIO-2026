package ar.utn.donatrack.brokerlogistica.adapters;

import ar.utn.donatrack.brokerlogistica.dtos.DireccionDTO;
import ar.utn.donatrack.brokerlogistica.dtos.DonacionEnvioDTO;
import ar.utn.donatrack.brokerlogistica.dtos.EnvioAsignadoDTO;
import ar.utn.donatrack.brokerlogistica.exceptions.ProveedorNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter de servicio-logistica (el propio).
 *
 * Contrato del proveedor: un único POST /api/logistica/planificaciones con
 * todas las donaciones; logística las particiona en lotes, el proveedor
 * reparte las donaciones entre los camiones y responde las rutas. El id de
 * seguimiento de cada donación es el id de su entrega.
 */
@Component
public class DonatrackLogisticaAdapter extends AdapterLogisticaHttp {

    public static final String NOMBRE = "DONATRACK";

    public DonatrackLogisticaAdapter(
            RestClient.Builder builder,
            @Value("${broker.proveedores.donatrack.url}") String baseUrl) {
        super(builder, baseUrl);
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public List<EnvioAsignadoDTO> solicitarEnvios(List<DonacionEnvioDTO> donaciones) {
        List<LoteRespuesta> lotes;
        try {
            lotes = restClient.post()
                    .uri("/api/logistica/planificaciones")
                    .body(new PlanificacionRequest(donaciones.stream().map(this::aDonacionParaRutear).toList()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            throw new ProveedorNoDisponibleException(NOMBRE, e);
        }
        if (lotes == null || lotes.isEmpty()) {
            throw new ProveedorNoDisponibleException(NOMBRE, new IllegalStateException("Respuesta sin lotes"));
        }
        return asignarEntregas(donaciones, lotes);
    }

    /**
     * Cada donación del pedido tiene que aparecer exactamente una vez, con el
     * id de su entrega. El resultado sale en el mismo orden del pedido. Si
     * falta una donación o se repite, falla la llamada entera.
     */
    private List<EnvioAsignadoDTO> asignarEntregas(List<DonacionEnvioDTO> donaciones, List<LoteRespuesta> lotes) {
        Map<UUID, UUID> entregaPorDonacion = new HashMap<>();
        for (LoteRespuesta lote : lotes) {
            if (lote.rutas() == null) {
                continue;
            }
            for (RutaRespuesta ruta : lote.rutas()) {
                if (ruta.paradas() == null) {
                    continue;
                }
                for (ParadaRespuesta parada : ruta.paradas()) {
                    if (parada.entregas() == null) {
                        continue;
                    }
                    for (EntregaRespuesta entrega : parada.entregas()) {
                        if (entrega.id() == null || entrega.idDonacion() == null) {
                            throw new ProveedorNoDisponibleException(NOMBRE,
                                    new IllegalStateException("La respuesta de logística trae una entrega sin id"));
                        }
                        UUID anterior = entregaPorDonacion.put(entrega.idDonacion(), entrega.id());
                        if (anterior != null) {
                            throw new ProveedorNoDisponibleException(NOMBRE,
                                    new IllegalStateException("La donación " + entrega.idDonacion() + " aparece dos veces"));
                        }
                    }
                }
            }
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<EnvioAsignadoDTO> envios = new ArrayList<>();
        for (DonacionEnvioDTO donacion : donaciones) {
            UUID idEntrega = entregaPorDonacion.get(donacion.idDonacion());
            if (idEntrega == null) {
                throw new ProveedorNoDisponibleException(NOMBRE,
                        new IllegalStateException("La donación " + donacion.idDonacion() + " no tiene entrega"));
            }
            envios.add(new EnvioAsignadoDTO(donacion.idDonacion(), NOMBRE, idEntrega.toString(), ahora));
        }
        return envios;
    }

    private DonacionParaRutear aDonacionParaRutear(DonacionEnvioDTO donacion) {
        return new DonacionParaRutear(donacion.idDonacion(), donacion.idEntidadBeneficiaria(), donacion.direccionEntrega());
    }

    // Sin camionesIds: logística usa los camiones DISPONIBLE de su flota.
    private record PlanificacionRequest(List<DonacionParaRutear> donaciones) {
    }

    private record DonacionParaRutear(UUID idDonacion, UUID idEntidadBeneficiaria, DireccionDTO direccionEntrega) {
    }

    private record LoteRespuesta(UUID id, int cantidadDonaciones, List<RutaRespuesta> rutas) {
    }

    private record RutaRespuesta(UUID id, UUID camionId, List<ParadaRespuesta> paradas) {
    }

    private record ParadaRespuesta(List<EntregaRespuesta> entregas) {
    }

    private record EntregaRespuesta(UUID id, UUID idDonacion) {
    }
}
