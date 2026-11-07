package ar.utn.donatrack.logistica.integracion;

import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.interfaces.integracion.EntregaEventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Observer (sujeto): Spring inyecta automáticamente todos los beans que
 * implementan EntregaEventListener. Agregar un nuevo canal de aviso
 * (ej. otro webhook, un log de auditoría) no requiere tocar esta clase.
 */
@Component
public class EntregaEventPublisher {

    private final List<EntregaEventListener> listeners;

    public EntregaEventPublisher(List<EntregaEventListener> listeners) {
        this.listeners = listeners;
    }

    public void publicar(EntregaEvento evento) {
        listeners.forEach(listener -> listener.onEvento(evento));
    }
}
