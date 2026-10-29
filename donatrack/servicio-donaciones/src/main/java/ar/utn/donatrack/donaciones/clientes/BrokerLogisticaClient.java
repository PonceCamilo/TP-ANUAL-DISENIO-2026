package ar.utn.donatrack.donaciones.clientes;

import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.bind.annotation.RequestBody;
import ar.utn.donatrack.donaciones.config.ClientesHttp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Cliente HTTP del broker de integración con logística (puerto 8087).
 *
 * Donaciones NO le habla directo a ningún servicio de logística: le manda las
 * donaciones al broker y el broker elige el proveedor (DONATRACK o EXTERNA),
 * traduce el pedido al contrato de cada uno y, a la vuelta, traduce los eventos
 * al formato de los callbacks de /logistica/eventos/*.
 *
 * DIFERENCIA IMPORTANTE con NotificacionClient e IncentivosClient: esos dos dejan
 * el aviso en la bandeja de salida y se envía después, con reintentos. Acá la
 * llamada es sincrónica y necesita la respuesta (qué proveedor tomó cada
 * donación): si el envío no salió, Donaciones TIENE que saberlo para no marcar la
 * donación como lista para entregar. Por eso las excepciones se propagan.
 *
 * Con timeouts (spring.http.client.*): un broker colgado ya no deja colgado al
 * despacho.
 */
@Component
public class BrokerLogisticaClient {

    /** El endpoint del broker que usa donaciones. */
    @HttpExchange
    public interface Api {

        @PostExchange("/api/broker/envios")
        ResultadoEnvio solicitarEnvio(@RequestBody SolicitudEnvio solicitud);
    }

    private final Api api;

    public BrokerLogisticaClient(RestClient.Builder builder,
            @Value("${servicios.broker-logistica.url:http://localhost:8087}") String baseUrl) {
        this.api = ClientesHttp.crear(builder, baseUrl, Api.class);
    }

    /**
     * Pide al broker que despache las donaciones indicadas.
     *
     * @param proveedor "DONATRACK" o "EXTERNA" para forzar uno; null para que el
     *                  broker elija por prioridad y haga fallback si el primero
     *                  no está disponible.
     * @throws org.springframework.web.client.RestClientResponseException si el
     *         broker responde 4xx/5xx. El caso esperable es 503: ningún proveedor
     *         disponible.
     * @throws org.springframework.web.client.ResourceAccessException si el broker
     *         no responde (connection refused, timeout).
     */
    public ResultadoEnvio solicitarEnvio(String proveedor, List<DonacionEnvio> donaciones) {
        return api.solicitarEnvio(new SolicitudEnvio(proveedor, donaciones));
    }

    // ── Contrato del broker ────────────────────────────────────────────────────
    // Replican los records de servicio-broker-logistica. No se comparten clases
    // entre servicios a propósito: cada uno mantiene su propio modelo.

    public record SolicitudEnvio(
            String proveedor,
            List<DonacionEnvio> donaciones) {}

    public record DonacionEnvio(
            UUID idDonacion,
            UUID idEntidadBeneficiaria,
            String nombreEntidad,
            DireccionEnvio direccionEntrega) {}

    /** calle, localidad y provincia son obligatorios para el broker; codigoPostal es opcional. */
    public record DireccionEnvio(
            String calle,
            int numero,
            String localidad,
            String provincia,
            String codigoPostal) {}

    /** proveedoresDescartados muestra el fallback en acción: queda vacío si atendió el primero. */
    public record ResultadoEnvio(
            String proveedor,
            List<String> proveedoresDescartados,
            List<EnvioAsignado> envios) {}

    /** idSeguimiento es el id con el que el proveedor sigue la donación (id de la entrega en DONATRACK, trackingId en EXTERNA). */
    public record EnvioAsignado(
            UUID idDonacion,
            String proveedor,
            String idSeguimiento,
            LocalDateTime fechaAsignacion) {}
}
