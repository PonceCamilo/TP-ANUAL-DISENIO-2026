package ar.utn.donatrack.donaciones.config;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Arma los clientes HTTP hacia los otros servicios a partir de una interfaz
 * declarativa (@HttpExchange): la interfaz dice qué endpoint se llama y Spring
 * arma la llamada. Antes cada cliente armaba la petición a mano con
 * RestClient.create(url).
 *
 * Se arman sobre el RestClient.Builder de Spring Boot, que trae:
 *  - los timeouts de spring.http.client.connect-timeout / read-timeout. Antes no
 *    había ninguno: un servicio lento dejaba colgado el hilo que lo llamaba.
 *  - el ObjectMapper de la aplicación, así las fechas viajan en ISO-8601 y no
 *    como arreglos.
 *
 * Cada llamada lleva el id de correlación del pedido en curso (IdDeCorrelacion),
 * para seguirla en los logs del otro servicio.
 *
 * Los reintentos no van acá: los avisos los reintenta la bandeja de salida y el
 * broker de logística ya hace fallback entre proveedores.
 */
public final class ClientesHttp {

    private ClientesHttp() {
    }

    public static <T> T crear(RestClient.Builder builder, String urlBase, Class<T> api) {
        RestClient cliente = builder.baseUrl(urlBase).requestInterceptor(IdDeCorrelacion.propagador()).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(cliente)).build().createClient(api);
    }
}
