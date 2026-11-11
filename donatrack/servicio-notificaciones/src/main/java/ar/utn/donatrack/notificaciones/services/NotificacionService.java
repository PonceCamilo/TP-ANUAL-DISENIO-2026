package ar.utn.donatrack.notificaciones.services;

import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;
import ar.utn.donatrack.notificaciones.exceptions.NotificacionNoEncontradaException;
import ar.utn.donatrack.notificaciones.factory.NotificadorFactory;
import ar.utn.donatrack.notificaciones.interfaces.repositories.NotificacionRepositoryInterface;
import ar.utn.donatrack.notificaciones.interfaces.services.NotificacionServiceInterface;
import ar.utn.donatrack.notificaciones.interfaces.services.NotificadorInterface;
import ar.utn.donatrack.notificaciones.model.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Orquesta el envío de notificaciones: construye la Notificacion, la persiste,
 * delega el envío al notificador correcto (resuelto por la NotificadorFactory)
 * y vuelve a persistir el resultado final (ENVIADA / FALLIDA).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService implements NotificacionServiceInterface {

    private final NotificacionRepositoryInterface repositorio;
    private final NotificadorFactory notificadorFactory;

    public void enviar(SolicitudNotificacionDto solicitud) {
        // La solicitud trae el medio como String; la factory lo traduce al
        // MedioNotificacion concreto para que el dominio siga tipado (Strategy/Factory).
        NotificadorInterface notificador = notificadorFactory.obtenerPara(solicitud.medio());

        Notificacion notificacion = Notificacion.builder()
                .id(UUID.randomUUID())
                .destinatario(solicitud.destinatario())
                .mensaje(solicitud.mensaje())
                .evento(solicitud.evento())
                .medio(notificador.getMedio())
                .estado(EstadoNotificacion.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .build();

        repositorio.guardar(notificacion);

        // El notificador (Template Method) actualiza el estado a ENVIADA o FALLIDA.
        notificador.enviar(notificacion);

        repositorio.guardar(notificacion);
        log.info("Notificación {} registrada con estado: {}", notificacion.getId(), notificacion.getEstado());
    }

    public List<Notificacion> obtenerTodas() {
        return repositorio.buscarTodas();
    }

    public Notificacion obtenerPorId(UUID id) {
        Notificacion notificacion = repositorio.buscarPorId(id);
        if (notificacion == null) {
            throw new NotificacionNoEncontradaException(id);
        }
        return notificacion;
    }

    /** Marca una notificación puntual como leída (ej. la abrieron desde un link directo). */
    public void marcarLeida(UUID id) {
        Notificacion notificacion = obtenerPorId(id);
        notificacion.marcarLeida();
        repositorio.guardar(notificacion);
    }

    /**
     * Marca como leídas todas las notificaciones pendientes de un conjunto de
     * destinatarios (ej. el usuario abrió la pantalla de notificaciones: todo
     * lo que tenía sin leer pasa a leído de una vez).
     */
    public void marcarLeidas(List<String> destinatarios) {
        List<Notificacion> notificaciones = repositorio.buscarPorDestinatarios(destinatarios);
        for (Notificacion notificacion : notificaciones) {
            if (!notificacion.isLeida()) {
                notificacion.marcarLeida();
                repositorio.guardar(notificacion);
            }
        }
    }
}
