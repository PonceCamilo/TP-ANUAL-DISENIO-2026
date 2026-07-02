package ar.utn.donatrack.notificaciones.interfaces.services;

import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;

/**
 * ══════════════════════════════════════════════════════════════
 * PATRÓN STRATEGY
 * ══════════════════════════════════════════════════════════════
 * Hay tres canales (email, sms, whatsapp). Cada uno "hace lo mismo"
 * desde afuera (enviar), pero distinto adentro. Strategy permite
 * intercambiarlos sin tocar el servicio que los usa.
 *
 * getMedio() permite que la NotificadorFactory arme el mapa
 * medio → notificador automáticamente, registrando cada implementación.
 */
public interface NotificadorInterface {

    /** Intenta enviar la notificación y actualiza su estado. */
    void enviar(Notificacion notificacion);

    /** Indica qué medio maneja este notificador. */
    MedioNotificacion  getMedio();
}
