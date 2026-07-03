package ar.utn.donatrack.notificaciones.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Decisión de diseño: no se recibe el objeto MedioDeContacto del dominio
 * de Donaciones para no acoplar este microservicio a un modelo ajeno.
 * Cada servicio mantiene su propio modelo (Bounded Context independiente).
 *
 * `medio` viaja como String ("EMAIL"/"SMS"/"WHATSAPP"): los clientes (Donaciones,
 * Incentivos) no conocen la jerarquía MedioNotificacion de este servicio, y así
 * Jackson deserializa sin necesitar type info sobre una clase abstracta. La
 * NotificadorFactory resuelve el MedioNotificacion concreto a partir del nombre.
 */
public record SolicitudNotificacionDto(
        @NotBlank String destinatario,
        @NotBlank String mensaje,
        @NotBlank String medio
) {}
