package ar.utn.donatrack.notificaciones.model.medios;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Reconstruye el objeto de medio a partir de su nombre.
 *
 * POR QUÉ EXISTE: MedioNotificacion es una jerarquía de clases, no un enum, y
 * esos objetos NO se persisten. En la base se guarda únicamente el nombre
 * ("EMAIL", "SMS", "WHATSAPP", "DISCORD") y al cargar la notificación se vuelve
 * a instanciar la clase correspondiente.
 *
 * Es el mismo criterio que usa servicio-donaciones para el patrón State: la
 * jerarquía existe por el comportamiento, no por los datos, así que lo único
 * que tiene sentido guardar es el discriminador.
 *
 * TIENE QUE ESTAR COMPLETO. Este mapa debe cubrir TODOS los medios que algún
 * notificador pueda devolver. Si falta uno, la notificación se guarda sin
 * problema y falla AL LEERLA, desde el @PostLoad — y como buscarTodas() las
 * carga todas, una sola fila con el medio faltante rompe el historial entero.
 * Ya pasó con DISCORD. El test NotificacionRepositoryTest.cadaMedioSeRehidrata
 * recorre los cuatro medios justamente para que no vuelva a pasar.
 */
public final class MediosNotificacion {

    private static final Map<String, Supplier<MedioNotificacion>> POR_NOMBRE = Map.of(
            "EMAIL", Email::new,
            "SMS", Sms::new,
            "WHATSAPP", WhatsApp::new,
            "DISCORD", Discord::new);

    private MediosNotificacion() {
    }

    public static MedioNotificacion desde(String nombre) {
        Supplier<MedioNotificacion> factory = POR_NOMBRE.get(nombre);
        if (factory == null) {
            throw new IllegalArgumentException("Medio de notificación desconocido: " + nombre);
        }
        return factory.get();
    }

    /** Tolerante a null: si la columna está vacía devuelve null en lugar de fallar. */
    public static MedioNotificacion desdeONull(String nombre) {
        return nombre == null ? null : desde(nombre);
    }
}
