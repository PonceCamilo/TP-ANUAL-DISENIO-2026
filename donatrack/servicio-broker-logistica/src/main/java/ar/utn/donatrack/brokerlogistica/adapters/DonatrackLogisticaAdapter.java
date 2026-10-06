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
import java.util.List;
import java.util.UUID;

/**
 * Adapter de servicio-logistica (el propio).
 *
 * Contrato del proveedor: un único POST /api/logistica/planificaciones con
 * todas las donaciones; logística las particiona en lotes, elige los camiones
 * disponibles y responde la lista de lotes planificados. El id de seguimiento
 * de cada donación es el id del lote que la contiene.
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
        return asignarLotes(donaciones, lotes);
    }

    /**
     * Logística particiona respetando el orden recibido, así que los lotes
     * vienen en orden y cada uno trae cuántas donaciones contiene.
     */
    private List<EnvioAsignadoDTO> asignarLotes(List<DonacionEnvioDTO> donaciones, List<LoteRespuesta> lotes) {
        LocalDateTime ahora = LocalDateTime.now();
        List<EnvioAsignadoDTO> envios = new ArrayList<>();
        int indice = 0;
        for (LoteRespuesta lote : lotes) {
            for (int i = 0; i < lote.cantidadDonaciones() && indice < donaciones.size(); i++, indice++) {
                envios.add(new EnvioAsignadoDTO(donaciones.get(indice).idDonacion(), NOMBRE, lote.id().toString(), ahora));
            }
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

    private record LoteRespuesta(UUID id, int cantidadDonaciones) {
    }
}
