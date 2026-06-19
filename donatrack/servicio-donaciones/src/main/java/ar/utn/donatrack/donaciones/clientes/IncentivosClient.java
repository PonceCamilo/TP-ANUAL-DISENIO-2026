package ar.utn.donatrack.donaciones.clientes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * Cliente HTTP que llama al servicio-incentivos (puerto 8083).
 *
 *  - Cuando el donante registra una donación → notifica a incentivos para que
 *    actualice métricas y verifique misiones completadas.
 *  - Cuando una donación es entregada exitosamente → notifica a incentivos para
 *    que actualice el contador de donaciones exitosas / organizaciones ayudadas.
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
                                            int cantidadBienes, List<String> categoriasDonadas) {
        try {
            restClient.post()
                    .uri("/api/incentivos/donantes/donacion")
                    .body(new ProcesarDonacionRequest(donanteId, destinatario, medio, cantidadBienes, categoriasDonadas))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[IncentivosClient] Donación registrada enviada para donante {}", donanteId);
        } catch (Exception e) {
            log.error("[IncentivosClient] Error al notificar donación: {}", e.getMessage());
        }
    }

    public void notificarDonacionExitosa(UUID donanteId, String destinatario, String medio) {
        try {
            restClient.post()
                    .uri("/api/incentivos/donantes/donacion-exitosa")
                    .body(new ProcesarDonacionRequest(donanteId, destinatario, medio, 0, List.of()))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[IncentivosClient] Donación exitosa enviada para donante {}", donanteId);
        } catch (Exception e) {
            log.error("[IncentivosClient] Error al notificar donación exitosa: {}", e.getMessage());
        }
    }

    private record ProcesarDonacionRequest(
            UUID donanteId, String destinatario, String medio,
            int cantidadBienes, List<String> categoriasDonadas) {}
}
