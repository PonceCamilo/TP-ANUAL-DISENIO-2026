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
        Notificacion notificacion = Notificacion.builder()
                .id(UUID.randomUUID())
                .destinatario(solicitud.destinatario())
                .mensaje(solicitud.mensaje())
                .medio(solicitud.medio())
                .evento(solicitud.evento())
                .estado(EstadoNotificacion.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .build();

        repositorio.guardar(notificacion);

        // El notificador (Template Method) actualiza el estado a ENVIADA o FALLIDA.
        NotificadorInterface notificador = notificadorFactory.obtenerPara(notificacion.getMedio());
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
}
