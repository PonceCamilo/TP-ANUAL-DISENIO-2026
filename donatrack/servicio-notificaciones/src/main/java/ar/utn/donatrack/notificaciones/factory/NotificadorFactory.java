package ar.utn.donatrack.notificaciones.factory;

import ar.utn.donatrack.notificaciones.interfaces.services.NotificadorInterface;
import ar.utn.donatrack.notificaciones.model.TipoMedioNotificacion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ══════════════════════════════════════════════════════════════
 * PATRÓN FACTORY METHOD
 * ══════════════════════════════════════════════════════════════
 * El servicio no necesita saber qué implementación concreta usar:
 * solo pide "dame el notificador para EMAIL" y la factory lo resuelve.
 *
 * Spring inyecta automáticamente todos los NotificadorInterface disponibles.
 * Si se agrega un canal nuevo (ej: Telegram), solo se crea la clase y se
 * registra sola — sin tocar la factory ni el servicio.
 */
@Component
public class NotificadorFactory {

    private final Map<TipoMedioNotificacion, NotificadorInterface> notificadores;

    public NotificadorFactory(List<NotificadorInterface> notificadores) {
        this.notificadores = notificadores.stream()
                .collect(Collectors.toMap(NotificadorInterface::getMedio, Function.identity()));
    }

    public NotificadorInterface obtenerPara(TipoMedioNotificacion medio) {
        NotificadorInterface notificador = notificadores.get(medio);
        if (notificador == null) {
            throw new IllegalArgumentException(
                    "No existe un notificador registrado para el medio: " + medio);
        }
        return notificador;
    }
}
