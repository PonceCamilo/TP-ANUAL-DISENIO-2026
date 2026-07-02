package ar.utn.donatrack.notificaciones.dto;

//import ar.utn.donatrack.notificaciones.model.TipoEvento;
import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Decisión de diseño: no se recibe el objeto MedioDeContacto del dominio
 * de Donaciones para no acoplar este microservicio a un modelo ajeno.
 * Cada servicio mantiene su propio modelo (Bounded Context independiente).
 */
public record SolicitudNotificacionDto(
        @NotBlank String destinatario,
        @NotBlank String mensaje,
        @NotNull MedioNotificacion medio
       // @NotNull TipoEvento evento
) {}
