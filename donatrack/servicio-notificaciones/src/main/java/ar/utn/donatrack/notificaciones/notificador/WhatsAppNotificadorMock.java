package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ══════════════════════════════════════════════════════════════
 * PATRÓN STRATEGY — implementación concreta: WHATSAPP
 * ══════════════════════════════════════════════════════════════
 * Mock: simula envío de WhatsApp. En producción se integraría
 * con la API de Meta (WhatsApp Business API).
 */
@Component
public class WhatsAppNotificadorMock implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificadorMock.class);

    @Override
    public void enviar(Notificacion notificacion) {
        try {
            // ── SIMULACIÓN: en producción aquí iría llamada a WhatsApp Business API ──
            log.info("[MOCK WHATSAPP] Para: {} | Mensaje: {}",
                    notificacion.getDestinatario(),
                    notificacion.getMensaje());

            notificacion.marcarEnviada();

        } catch (Exception e) {
            log.error("[MOCK WHATSAPP] Falló el envío a {}: {}", notificacion.getDestinatario(), e.getMessage());
            notificacion.marcarFallida();
        }
    }

    @Override
    public MedioNotificacion getMedio() {
        return MedioNotificacion.WHATSAPP;
    }
}
