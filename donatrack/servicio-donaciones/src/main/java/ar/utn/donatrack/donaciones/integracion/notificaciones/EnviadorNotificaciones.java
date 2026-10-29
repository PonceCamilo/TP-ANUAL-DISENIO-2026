package ar.utn.donatrack.donaciones.integracion.notificaciones;

import ar.utn.donatrack.donaciones.interfaces.integracion.NotificacionTransport;
import ar.utn.donatrack.donaciones.salida.Destinos;
import ar.utn.donatrack.donaciones.salida.EnviadorDeMensajes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Envía los avisos de la bandeja de salida por el transporte configurado (REST o RabbitMQ). */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnviadorNotificaciones implements EnviadorDeMensajes {

    private final NotificacionTransport transporte;
    private final ObjectMapper json;

    /** Deja constancia en el arranque de qué transporte quedó activo. */
    @PostConstruct
    public void registrarTransporteActivo() {
        log.info("[Notificaciones] Transporte de notificaciones: {}", transporte.nombre());
    }

    @Override
    public Set<String> destinos() {
        return Set.of(Destinos.NOTIFICACION);
    }

    @Override
    public void enviar(String destino, String payload) throws Exception {
        transporte.enviar(json.readValue(payload, SolicitudNotificacionMensaje.class));
    }
}
