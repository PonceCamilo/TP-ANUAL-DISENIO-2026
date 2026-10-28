package ar.utn.donatrack.donaciones.integracion.incentivos;

import ar.utn.donatrack.donaciones.salida.Destinos;
import ar.utn.donatrack.donaciones.salida.EnviadorDeMensajes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

/**
 * Envía al Servicio de Incentivos los avisos de la bandeja de salida.
 *
 * Manda el JSON tal como quedó guardado: lo armó el ObjectMapper de Spring, así
 * que las fechas viajan en ISO-8601 (RestClient.create() las serializaba como
 * arreglos).
 */
@Component
public class EnviadorIncentivos implements EnviadorDeMensajes {

    private static final Map<String, String> RUTAS = Map.of(
            Destinos.INCENTIVOS_DONACION_REGISTRADA, "/api/incentivos/donantes/donacion",
            Destinos.INCENTIVOS_DONACION_EXITOSA, "/api/incentivos/donantes/donacion-exitosa");

    private final RestClient restClient;

    public EnviadorIncentivos(@Value("${servicios.incentivos.url:http://localhost:8083}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public Set<String> destinos() {
        return RUTAS.keySet();
    }

    @Override
    public void enviar(String destino, String payload) {
        restClient.post()
                .uri(RUTAS.get(destino))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
