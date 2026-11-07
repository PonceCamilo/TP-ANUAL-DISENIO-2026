package ar.utn.donatrack.donaciones.integracion.incentivos;

import ar.utn.donatrack.donaciones.config.ClientesHttp;
import ar.utn.donatrack.donaciones.salida.Destinos;
import ar.utn.donatrack.donaciones.salida.EnviadorDeMensajes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Envía al Servicio de Incentivos los avisos de la bandeja de salida.
 *
 * Manda el JSON tal como quedó guardado: lo armó el ObjectMapper de Spring, así
 * que las fechas viajan en ISO-8601.
 */
@Component
public class EnviadorIncentivos implements EnviadorDeMensajes {

    /** Los endpoints de incentivos que usa donaciones (puerto 8083). */
    @HttpExchange(url = "/api/incentivos/donantes", contentType = MediaType.APPLICATION_JSON_VALUE)
    public interface Api {

        @PostExchange("/donacion")
        void donacionRegistrada(@RequestBody String solicitud);

        @PostExchange("/donacion-exitosa")
        void donacionExitosa(@RequestBody String solicitud);
    }

    private final Map<String, Consumer<String>> envios;

    public EnviadorIncentivos(RestClient.Builder builder,
                              @Value("${servicios.incentivos.url:http://localhost:8083}") String baseUrl) {
        Api api = ClientesHttp.crear(builder, baseUrl, Api.class);
        this.envios = Map.of(
                Destinos.INCENTIVOS_DONACION_REGISTRADA, api::donacionRegistrada,
                Destinos.INCENTIVOS_DONACION_EXITOSA, api::donacionExitosa);
    }

    @Override
    public Set<String> destinos() {
        return envios.keySet();
    }

    @Override
    public void enviar(String destino, String payload) {
        envios.get(destino).accept(payload);
    }
}
