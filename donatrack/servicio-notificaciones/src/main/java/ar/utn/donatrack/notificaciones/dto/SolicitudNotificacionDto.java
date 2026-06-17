package ar.utn.donatrack.notificaciones.dto;

import ar.utn.donatrack.notificaciones.model.TipoEvento;
import ar.utn.donatrack.notificaciones.model.TipoMedioNotificacion;

/**
 * Decisión de diseño: no se recibe el objeto MedioDeContacto del dominio
 * de Donaciones para no acoplar este microservicio a un modelo ajeno.
 * Cada servicio mantiene su propio modelo (Bounded Context independiente).
 */
public record SolicitudNotificacionDto(
        String destinatario,
        String mensaje,
        TipoMedioNotificacion medio,
        TipoEvento evento
) {}