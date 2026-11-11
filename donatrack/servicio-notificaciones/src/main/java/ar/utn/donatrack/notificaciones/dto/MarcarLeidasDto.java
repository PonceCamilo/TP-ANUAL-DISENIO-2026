package ar.utn.donatrack.notificaciones.dto;

import java.util.List;

/**
 * Body del endpoint que marca como leídas todas las notificaciones de uno o
 * más destinatarios (el frontend lo llama con el email del usuario logueado
 * y, para donante/entidad, el resto de sus medios de contacto).
 */
public record MarcarLeidasDto(List<String> destinatarios) {
}
