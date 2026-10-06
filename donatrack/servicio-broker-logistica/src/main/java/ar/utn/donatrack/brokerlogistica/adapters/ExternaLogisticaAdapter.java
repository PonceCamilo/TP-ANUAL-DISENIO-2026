package ar.utn.donatrack.brokerlogistica.adapters;

import ar.utn.donatrack.brokerlogistica.dtos.DonacionEnvioDTO;
import ar.utn.donatrack.brokerlogistica.dtos.EnvioAsignadoDTO;
import ar.utn.donatrack.brokerlogistica.exceptions.ProveedorNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de servicio-logistica-externa (proveedor de terceros).
 *
 * Contrato del proveedor: un POST /api/v1/envios por donación, con
 * shipmentRef (acá, el id de la donación), un destinatario legible y el
 * domicilio en una sola línea. Responde un trackingId por envío.
 *
 * Si el proveedor falla a mitad de camino, los envíos ya creados quedan
 * registrados en él; el broker reporta el fallo y prueba con el siguiente.
 */
@Component
public class ExternaLogisticaAdapter extends AdapterLogisticaHttp {

    public static final String NOMBRE = "EXTERNA";

    public ExternaLogisticaAdapter(
            RestClient.Builder builder,
            @Value("${broker.proveedores.externa.url}") String baseUrl) {
        super(builder, baseUrl);
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public List<EnvioAsignadoDTO> solicitarEnvios(List<DonacionEnvioDTO> donaciones) {
        List<EnvioAsignadoDTO> envios = new ArrayList<>();
        for (DonacionEnvioDTO donacion : donaciones) {
            EnvioRespuesta respuesta;
            try {
                respuesta = restClient.post()
                        .uri("/api/v1/envios")
                        .body(aEnvioRequest(donacion))
                        .retrieve()
                        .body(EnvioRespuesta.class);
            } catch (Exception e) {
                throw new ProveedorNoDisponibleException(NOMBRE, e);
            }
            if (respuesta == null || respuesta.trackingId() == null) {
                throw new ProveedorNoDisponibleException(NOMBRE, new IllegalStateException("Respuesta sin trackingId"));
            }
            envios.add(new EnvioAsignadoDTO(donacion.idDonacion(), NOMBRE, respuesta.trackingId(), LocalDateTime.now()));
        }
        return envios;
    }

    private EnvioRequest aEnvioRequest(DonacionEnvioDTO donacion) {
        String destinatario = donacion.nombreEntidad() != null && !donacion.nombreEntidad().isBlank()
                ? donacion.nombreEntidad()
                : "Entidad beneficiaria " + donacion.idEntidadBeneficiaria();
        return new EnvioRequest(
                donacion.idDonacion().toString(),
                destinatario,
                donacion.direccionEntrega().enUnaLinea());
    }

    private record EnvioRequest(String shipmentRef, String destinatario, String domicilio) {
    }

    private record EnvioRespuesta(String trackingId, String estado) {
    }
}
