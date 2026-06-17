package ar.utn.donatrack.notificaciones.service.impl;

import ar.utn.donatrack.notificaciones.factory.NotificadorFactory;
import ar.utn.donatrack.notificaciones.model.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.notificador.Notificador;
import ar.utn.donatrack.notificaciones.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionServiceImpl.class);

    private final NotificadorFactory factory;

    // Registro en memoria de todas las notificaciones (auditoría)
    // CopyOnWriteArrayList es thread-safe para lecturas concurrentes
    private final List<Notificacion> historial = new CopyOnWriteArrayList<>();

    public NotificacionServiceImpl(NotificadorFactory factory) {
        this.factory = factory;
    }

    @Override
    public Notificacion enviar(String destinatario, String mensaje, MedioNotificacion medio) {
        Notificacion notificacion = new Notificacion(destinatario, mensaje, medio);

        // ── Usa la factory para obtener el notificador correcto (Strategy + Factory Method) ──
        Notificador notificador = factory.obtenerPara(medio);
        notificador.enviar(notificacion); // el mock loguea y marca como ENVIADA

        historial.add(notificacion);
        log.info("Notificación {} registrada con estado: {}", notificacion.getId(), notificacion.getEstado());

        return notificacion;
    }

    @Override
    public List<Notificacion> listarTodas() {
        return Collections.unmodifiableList(historial);
    }
}
