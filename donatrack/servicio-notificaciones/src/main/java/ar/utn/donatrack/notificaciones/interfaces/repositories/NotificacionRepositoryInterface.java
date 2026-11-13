package ar.utn.donatrack.notificaciones.interfaces.repositories;

import ar.utn.donatrack.notificaciones.model.Notificacion;

import java.util.List;
import java.util.UUID;

public interface NotificacionRepositoryInterface {
    void guardar(Notificacion notificacion);
    List<Notificacion> buscarTodas();
    Notificacion buscarPorId(UUID id);
    List<Notificacion> buscarPorDestinatarios(List<String> destinatarios);

    // Entrega 6: una página, de la más nueva a la más vieja. destinatarios null: todas.
    org.springframework.data.domain.Page<Notificacion> buscarPagina(List<String> destinatarios,
                                                                    org.springframework.data.domain.Pageable pagina);

    long contarNoLeidas(List<String> destinatarios);
}