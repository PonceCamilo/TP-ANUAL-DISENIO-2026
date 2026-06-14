package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ══════════════════════════════════════════════════════════════
 * PATRÓN STRATEGY — implementación concreta: SMS
 * ══════════════════════════════════════════════════════════════
 * Mock: simula envío de SMS. En producción se integraría con
 * Twilio, AWS SNS u otro proveedor de SMS.
 */
@Component
public class SmsNotificadorMock implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificadorMock.class);

    @Override
    public void enviar(Notificacion notificacion) {
        try {
            // ── SIMULACIÓN: en producción aquí iría llamada a Twilio/SNS ──
            log.info("[MOCK SMS] Para: {} | Mensaje: {}",
                    notificacion.getDestinatario(),
                    notificacion.getMensaje());

            notificacion.marcarEnviada();

        } catch (Exception e) {
            log.error("[MOCK SMS] Falló el envío a {}: {}", notificacion.getDestinatario(), e.getMessage());
            notificacion.marcarFallida();
        }
    }

    @Override
    public MedioNotificacion getMedio() {
        return MedioNotificacion.SMS;
    }
}
