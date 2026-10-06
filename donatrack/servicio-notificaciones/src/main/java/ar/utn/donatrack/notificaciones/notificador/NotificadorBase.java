package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.interfaces.services.NotificadorInterface;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import lombok.extern.slf4j.Slf4j;

/**
 * ══════════════════════════════════════════════════════════════
 * PATRÓN TEMPLATE METHOD
 * ══════════════════════════════════════════════════════════════
 * Centraliza el esqueleto del envío (log + manejo de éxito/fallo).
 * Cada subclase concreta solo define:
 *   - getMedio()      : a qué medio responde (para la factory)
 *   - getEtiqueta()   : cómo se identifica en el log
 *   - realizarEnvio() : el paso variable del algoritmo — cómo se
 *                       despacha el mensaje realmente (SMTP, webhook,
 *                       o una simulación por consola).
 *
 * Antes este paso estaba mockeado directamente acá adentro. Al
 * necesitar canales reales (Gmail, Discord) además de los simulados
 * (SMS, WhatsApp), se extrajo como método abstracto: el esqueleto
 * (loguear intento, marcar ENVIADA/FALLIDA) no cambió en absoluto.
 *
 * Mismo enfoque que AlgoritmoAsignacionBase en servicio-donaciones.
 */
@Slf4j
public abstract class NotificadorBase implements NotificadorInterface {

    public void enviar(Notificacion notificacion) {
        try {
            realizarEnvio(notificacion);
            log.info("[{}] Enviado a: {} | Mensaje: {}",
                    getEtiqueta(), notificacion.getDestinatario(), notificacion.getMensaje());
            notificacion.marcarEnviada();
        } catch (Exception e) {
            log.error("[{}] Falló el envío a {}: {}",
                    getEtiqueta(), notificacion.getDestinatario(), e.getMessage());
            notificacion.marcarFallida();
        }
    }

    /**
     * Paso variable del Template Method: cómo se despacha el mensaje.
     * Puede ser una simulación (mock) o una integración real con un
     * proveedor externo. Si lanza una excepción, el esqueleto la
     * captura y marca la notificación como FALLIDA.
     */
    protected abstract void realizarEnvio(Notificacion notificacion) throws Exception;

    protected abstract String getEtiqueta();
}
