package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.interfaces.services.NotificadorInterface;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import lombok.extern.slf4j.Slf4j;

/**
 * ══════════════════════════════════════════════════════════════
 * PATRÓN TEMPLATE METHOD
 * ══════════════════════════════════════════════════════════════
 * Centraliza el esqueleto del envío (log + manejo de éxito/fallo).
 * Cada subclase concreta solo define dos cosas:
 *   - getMedio()   : a qué medio responde (para la factory)
 *   - getEtiqueta(): cómo se identifica en el log
 *
 * Mismo enfoque que AlgoritmoAsignacionBase en servicio-donaciones.
 */
@Slf4j
public abstract class NotificadorBase implements NotificadorInterface {

    public void enviar(Notificacion notificacion) {
        try {
            // ── SIMULACIÓN: en producción aquí iría el proveedor real (SMTP, Twilio, WhatsApp API) ──
            log.info("[MOCK {}] Para: {} | Mensaje: {}",
                    getEtiqueta(), notificacion.getDestinatario(), notificacion.getMensaje());
            notificacion.marcarEnviada();
        } catch (Exception e) {
            log.error("[MOCK {}] Falló el envío a {}: {}",
                    getEtiqueta(), notificacion.getDestinatario(), e.getMessage());
            notificacion.marcarFallida();
        }
    }

    protected abstract String getEtiqueta();
}
