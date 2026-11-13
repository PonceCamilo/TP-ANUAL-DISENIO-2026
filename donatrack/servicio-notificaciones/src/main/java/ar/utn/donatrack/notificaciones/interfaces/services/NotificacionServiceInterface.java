package ar.utn.donatrack.notificaciones.interfaces.services;

import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;
import ar.utn.donatrack.notificaciones.model.Notificacion;

import java.util.List;
import java.util.UUID;

public interface NotificacionServiceInterface {
    void enviar(SolicitudNotificacionDto solicitud);
    List<Notificacion> obtenerTodas();
    Notificacion obtenerPorId(UUID id);
    void marcarLeida(UUID id);
    void marcarLeidas(List<String> destinatarios);

    // Entrega 6: páginas desde 1, de la más nueva a la más vieja. destinatarios null: todas.
    org.springframework.data.domain.Page<Notificacion> buscar(List<String> destinatarios, int pagina, int tamanio);

    long contarNoLeidas(List<String> destinatarios);
}