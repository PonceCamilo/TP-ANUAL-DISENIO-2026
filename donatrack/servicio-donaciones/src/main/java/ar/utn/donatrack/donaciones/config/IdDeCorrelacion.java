package ar.utn.donatrack.donaciones.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Id de correlación de cada pedido (Entrega 5).
 *
 * Cada pedido que llega trae su id en el encabezado X-Correlation-Id (si el que
 * llama ya tiene uno, por ejemplo el broker de logística) o recibe uno nuevo. El
 * id queda en el MDC mientras dura el pedido, así cada línea de log que genera
 * lo muestra (logging.pattern.level), vuelve en la respuesta y viaja en las
 * llamadas HTTP que el pedido hace a otros servicios (ClientesHttp). Con eso se
 * pueden seguir en los logs de todos los servicios los pasos de un mismo pedido.
 *
 * Un id recibido que no tiene la forma esperada se reemplaza: va a los logs y no
 * puede traer saltos de línea ni texto arbitrario.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdDeCorrelacion extends OncePerRequestFilter {

    public static final String ENCABEZADO = "X-Correlation-Id";
    public static final String CLAVE_MDC = "correlationId";

    private static final Pattern FORMA_VALIDA = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest pedido, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String recibido = pedido.getHeader(ENCABEZADO);
        String id = recibido != null && FORMA_VALIDA.matcher(recibido).matches() ? recibido : UUID.randomUUID().toString();
        MDC.put(CLAVE_MDC, id);
        respuesta.setHeader(ENCABEZADO, id);
        try {
            cadena.doFilter(pedido, respuesta);
        } finally {
            MDC.remove(CLAVE_MDC);
        }
    }

    /** Agrega a una llamada saliente el id del pedido en curso, si hay uno. */
    public static ClientHttpRequestInterceptor propagador() {
        return (pedido, cuerpo, ejecucion) -> {
            String id = MDC.get(CLAVE_MDC);
            if (id != null) {
                pedido.getHeaders().set(ENCABEZADO, id);
            }
            return ejecucion.execute(pedido, cuerpo);
        };
    }
}
