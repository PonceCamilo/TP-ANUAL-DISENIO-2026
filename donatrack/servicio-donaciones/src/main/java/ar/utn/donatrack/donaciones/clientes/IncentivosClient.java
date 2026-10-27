package ar.utn.donatrack.donaciones.clientes;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Cliente HTTP que llama al servicio-incentivos (puerto 8083).
 *
 *  - Cuando el donante registra una donación → notifica a incentivos para que
 *    actualice métricas y verifique misiones completadas.
 *  - Cuando una donación es entregada exitosamente → notifica a incentivos para
 *    que actualice el contador de donaciones exitosas / organizaciones ayudadas.
 *
 * Una sola firma por evento y la fecha siempre la indica quien llama: la del
 * registro o la de la entrega. Antes, la donación exitosa se informaba con la
 * fecha en que se había registrado la donación y no con la de la entrega, y el
 * registro tomaba la hora de la zona del servidor en lugar de la de Argentina.
 *
 * Si incentivos no está levantado, loguea el error pero NO frena el flujo de donaciones.
 */
@Component
public class IncentivosClient {

    private static final Logger log = LoggerFactory.getLogger(IncentivosClient.class);

    private final RestClient restClient;

    public IncentivosClient(@Value("${servicios.incentivos.url:http://localhost:8083}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public void notificarDonacionRegistrada(UUID donanteId, String destinatario, String medio,
                                            int cantidadBienes, List<String> categoriasDonadas, LocalDateTime fecha) {
        enviar("/api/incentivos/donantes/donacion", "Donación registrada", new SolicitudIncentivo(
                donanteId, destinatario, medio, cantidadBienes, categoriasDonadas, fecha, null));
    }

    public void notificarDonacionExitosa(Donacion donacion, String destinatario, String medio, LocalDateTime fechaEntrega) {
        enviar("/api/incentivos/donantes/donacion-exitosa", "Donación exitosa", new SolicitudIncentivo(
                donacion.getIdDonante(), destinatario, medio,
                cantidadBienes(donacion), categoriasDonadas(donacion), fechaEntrega, entidadBeneficiaria(donacion)));
    }

    private void enviar(String ruta, String evento, SolicitudIncentivo solicitud) {
        try {
            restClient.post().uri(ruta).body(solicitud).retrieve().toBodilessEntity();
            log.info("[IncentivosClient] {} enviada para donante {}", evento, solicitud.donanteId());
        } catch (Exception e) {
            log.error("[IncentivosClient] Error al notificar {}: {}", evento.toLowerCase(), e.getMessage());
        }
    }

    private int cantidadBienes(Donacion donacion) {
        return donacion.getBienes().stream()
                .mapToInt(bien -> Math.max(1, bien.getCantidad()))
                .sum();
    }

    private List<String> categoriasDonadas(Donacion donacion) {
        return donacion.getBienes().stream()
                .map(Bien::getSubcategoria)
                .filter(Objects::nonNull)
                .map(Subcategoria::getTipo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String entidadBeneficiaria(Donacion donacion) {
        return donacion.getIdEntidadBeneficiaria() != null ? donacion.getIdEntidadBeneficiaria().toString() : null;
    }

    /** Cuerpo de los dos endpoints de incentivos: tienen los mismos campos. */
    record SolicitudIncentivo(
            UUID donanteId, String destinatario, String medio,
            int cantidadBienes, List<String> categoriasDonadas,
            LocalDateTime fecha, String entidadBeneficiaria) {}
}
