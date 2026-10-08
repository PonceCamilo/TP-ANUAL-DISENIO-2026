package ar.utn.donatrack.notificaciones.repositories;

import ar.utn.donatrack.notificaciones.interfaces.repositories.NotificacionRepositoryInterface;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.repositories.jpa.NotificacionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Persistencia de notificaciones en base relacional (Entrega 4).
 *
 * Antes era un ConcurrentHashMap: el historial de notificaciones se perdía en
 * cada reinicio del servicio, lo que vuelve inútil cualquier auditoría de qué
 * se envió y qué falló.
 *
 * La interfaz NotificacionRepositoryInterface no cambió, así que el service y
 * sus tests siguen igual. Esta clase es un adapter delgado entre el vocabulario
 * del dominio y el de Spring Data.
 */
@Repository
@RequiredArgsConstructor
public class NotificacionRepository implements NotificacionRepositoryInterface {

    private final NotificacionJpaRepository jpa;

    @Transactional
    public void guardar(Notificacion notificacion) {
        jpa.save(notificacion);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> buscarTodas() {
        return jpa.findAll();
    }

    /** Devuelve null y no Optional para no cambiar el contrato que ya usaba el service. */
    @Transactional(readOnly = true)
    public Notificacion buscarPorId(UUID id) {
        return jpa.findById(id).orElse(null);
    }
}
