package ar.utn.donatrack.notificaciones.dto.response;

import ar.utn.donatrack.notificaciones.model.Notificacion;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Una página de notificaciones (Entrega 6). Mismos campos que las páginas del
 * Servicio de Donaciones; la página empieza en 1.
 */
public record PaginaNotificacionesResponse(
        List<NotificacionResponse> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas
) {
    public static PaginaNotificacionesResponse desde(Page<Notificacion> pagina) {
        return new PaginaNotificacionesResponse(
                pagina.getContent().stream().map(NotificacionResponse::desde).toList(),
                pagina.getNumber() + 1,
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
