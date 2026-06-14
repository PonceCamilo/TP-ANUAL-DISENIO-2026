package ar.utn.donatrack.donaciones.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ════════════════════════════════════════════════════════════════════
 * INTERVENCIÓN DE PERSONA 4 EN EL CÓDIGO DE PERSONA 1
 * Archivo: servicio-donaciones/client/IncentivosClient.java  (NUEVO)
 * ════════════════════════════════════════════════════════════════════
 *
 * Cliente HTTP que llama al servicio-incentivos (puerto 8083).
 * Persona 1 no toca este archivo — Persona 4 lo agrega al proyecto.
 *
 * Lo que hace:
 *  - Cuando el donante registra una donación → notifica a incentivos
 *    para que actualice métricas y verifique misiones completadas.
 *  - Cuando una donación es entregada exitosamente → notifica a incentivos
 *    para que actualice el contador de donaciones exitosas.
 */
@Component
public class IncentivosClient {

    private static final Logger log = LoggerFactory.getLogger(IncentivosClient.class);

    private final RestClient restClient;

    public IncentivosClient(
            @Value("${servicios.incentivos.url:http://localhost:8083}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Llamar desde DonantServiceImpl (o DonacionService) justo después
     * de guardar una nueva donación en el repositorio.
     *
     * @param donanteId        UUID del donante
     * @param destinatario     email o número de contacto del donante
     * @param medio            "EMAIL", "SMS" o "WHATSAPP"
     * @param cantidadBienes   total de bienes en la donación
     * @param categoriasDonadas lista de nombres de categorías donadas
     */
    public void notificarDonacionRegistrada(UUID donanteId, String destinatario, String medio,
                                            int cantidadBienes, List<String> categoriasDonadas) {
        try {
            restClient.post()
                    .uri("/api/incentivos/donantes/donacion")
                    .body(Map.of(
                            "donanteId",        donanteId.toString(),
                            "destinatario",     destinatario,
                            "medio",            medio,
                            "cantidadBienes",   cantidadBienes,
                            "categoriasDonadas", categoriasDonadas
                    ))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[IncentivosClient] Donación registrada enviada para donante {}", donanteId);

        } catch (Exception e) {
            // No interrumpimos el flujo de donaciones si incentivos falla
            log.error("[IncentivosClient] Error al notificar donación: {}", e.getMessage());
        }
    }

    /**
     * Llamar desde DonacionService cuando el estado de la donación cambia a "ENTREGADA".
     *
     * @param donanteId    UUID del donante
     * @param destinatario email o número de contacto del donante
     * @param medio        "EMAIL", "SMS" o "WHATSAPP"
     */
    public void notificarDonacionExitosa(UUID donanteId, String destinatario, String medio) {
        try {
            restClient.post()
                    .uri("/api/incentivos/donantes/donacion-exitosa")
                    .body(Map.of(
                            "donanteId",      donanteId.toString(),
                            "destinatario",   destinatario,
                            "medio",          medio,
                            "cantidadBienes", 0,
                            "categoriasDonadas", List.of()
                    ))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[IncentivosClient] Donación exitosa enviada para donante {}", donanteId);

        } catch (Exception e) {
            log.error("[IncentivosClient] Error al notificar donación exitosa: {}", e.getMessage());
        }
    }
}
