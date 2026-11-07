package ar.utn.donatrack.notificaciones.repositories.jpa;

import ar.utn.donatrack.notificaciones.model.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Acceso JPA a la tabla notificacion.
 *
 * Es un detalle interno del paquete repositories: el service depende de
 * NotificacionRepositoryInterface, no de esta interfaz. Así, cambiar la
 * tecnología de persistencia no sale de este paquete.
 */
public interface NotificacionJpaRepository extends JpaRepository<Notificacion, UUID> {

    List<Notificacion> findByEstado(EstadoNotificacion estado);

    List<Notificacion> findByDestinatarioIgnoreCase(String destinatario);
}
