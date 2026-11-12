package ar.utn.donatrack.donaciones.integracion.autenticacion;

import ar.utn.donatrack.donaciones.config.ClientesHttp;
import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.salida.Destinos;
import ar.utn.donatrack.donaciones.salida.EnviadorDeMensajes;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Set;

/**
 * Envía al Servicio de Autenticación los avisos de personas importadas por CSV
 * (Entrega 6, ver AutenticacionClient). Manda el JSON tal como quedó guardado en
 * la bandeja de salida; si Autenticación no responde, RelayDeMensajes reintenta.
 *
 * El endpoint es una propuesta de contrato para el Servicio de Autenticación: por
 * eso el aviso está apagado por defecto (donaciones.cuentas.avisar-importados).
 */
@Component
public class EnviadorAutenticacion implements EnviadorDeMensajes {

    static final String ENCABEZADO_CLAVE = "X-DonaTrack-Servicio";

    @HttpExchange(url = "/api/autenticacion/cuentas", contentType = MediaType.APPLICATION_JSON_VALUE)
    public interface Api {
        @PostExchange("/importadas")
        void cuentasImportadas(@RequestHeader(name = ENCABEZADO_CLAVE, required = false) String clave,
                               @RequestBody String solicitud);
    }

    private final Api api;
    private final String clave;

    public EnviadorAutenticacion(RestClient.Builder builder, DonacionesProperties propiedades) {
        this.api = ClientesHttp.crear(builder, propiedades.cuentas().url(), Api.class);
        String configurada = propiedades.cuentas().clave();
        this.clave = configurada == null || configurada.isBlank() ? null : configurada;
    }

    @Override
    public Set<String> destinos() {
        return Set.of(Destinos.AUTENTICACION_CUENTAS_IMPORTADAS);
    }

    @Override
    public void enviar(String destino, String payload) {
        api.cuentasImportadas(clave, payload);
    }
}
