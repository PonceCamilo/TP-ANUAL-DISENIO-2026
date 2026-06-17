package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;

/**
 * ══════════════════════════════════════════════════════════════
 * PATRÓN STRATEGY
 * ══════════════════════════════════════════════════════════════
 * Por qué: hay tres canales distintos (email, SMS, WhatsApp).
 * Cada uno "hace lo mismo" desde afuera (enviar), pero de forma
 * diferente adentro. Strategy permite intercambiarlos sin tocar
 * el servicio que los usa.
 *
 * Si en el futuro se agrega Telegram, solo se crea una nueva
 * clase que implemente esta interfaz. El resto del código no cambia.
 */
public interface Notificador {

    /** Intenta enviar la notificación y actualiza su estado. */
    void enviar(Notificacion notificacion);

    /** Indica qué medio maneja este notificador. */
    MedioNotificacion getMedio();
}
