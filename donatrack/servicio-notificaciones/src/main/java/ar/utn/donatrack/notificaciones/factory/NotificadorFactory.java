package ar.utn.donatrack.notificaciones.factory;

import ar.utn.donatrack.notificaciones.model.MedioNotificacion;
import ar.utn.donatrack.notificaciones.notificador.Notificador;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ══════════════════════════════════════════════════════════════
 * PATRÓN FACTORY METHOD
 * ══════════════════════════════════════════════════════════════
 * Por qué: el servicio no necesita saber qué implementación
 * concreta usar. Solo pide "dame el notificador para EMAIL"
 * y la factory se lo devuelve.
 *
 * Spring inyecta automáticamente todos los Notificador disponibles.
 * Si se agrega un nuevo canal (ej: Telegram), solo se crea la clase
 * y se registra sola — sin tocar la factory ni el servicio.
 */
@Component
public class NotificadorFactory {

    // ── Mapa: medio → notificador concreto ─────────────────────────────────
    private final Map<MedioNotificacion, Notificador> notificadores;

    public NotificadorFactory(List<Notificador> notificadores) {
        // Spring inyecta todos los beans que implementen Notificador
        this.notificadores = notificadores.stream()
                .collect(Collectors.toMap(Notificador::getMedio, Function.identity()));
    }

    // ── FACTORY METHOD ───────────────────────────────────────────────────────
    public Notificador obtenerPara(MedioNotificacion medio) {
        Notificador notificador = notificadores.get(medio);
        if (notificador == null) {
            throw new IllegalArgumentException(
                    "No existe un notificador registrado para el medio: " + medio);
        }
        return notificador;
    }
}
