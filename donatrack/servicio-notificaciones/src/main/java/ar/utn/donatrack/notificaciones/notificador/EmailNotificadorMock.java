package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ══════════════════════════════════════════════════════════════
 * PATRÓN STRATEGY — implementación concreta: EMAIL
 * ══════════════════════════════════════════════════════════════
 * Mock: simula el envío de email por consola (log).
 * En una entrega futura se reemplaza por la llamada real a
 * un proveedor SMTP o servicio de email externo.
 */
@Component
public class EmailNotificadorMock implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificadorMock.class);

    @Override
    public void enviar(Notificacion notificacion) {
        try {
            // ── SIMULACIÓN: en producción aquí iría el cliente SMTP ──
            log.info("[MOCK EMAIL] Para: {} | Asunto: Notificación DonaTrack | Cuerpo: {}",
                    notificacion.getDestinatario(),
                    notificacion.getMensaje());

            notificacion.marcarEnviada(); // marca como enviada exitosamente

        } catch (Exception e) {
            log.error("[MOCK EMAIL] Falló el envío a {}: {}", notificacion.getDestinatario(), e.getMessage());
            notificacion.marcarFallida();
        }
    }

    @Override
    public MedioNotificacion getMedio() {
        return MedioNotificacion.EMAIL;
    }
}
