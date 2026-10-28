package ar.utn.donatrack.donaciones.repositories.jpa;

import ar.utn.donatrack.donaciones.salida.EstadoMensaje;
import ar.utn.donatrack.donaciones.salida.MensajeSaliente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Acceso a la bandeja de salida (ver MensajeSaliente). */
public interface MensajeSalienteJpaRepository extends JpaRepository<MensajeSaliente, UUID> {

    /** Los pendientes cuyo próximo intento ya llegó, los más viejos primero. */
    @Query("SELECT m FROM MensajeSaliente m WHERE m.estado = :estado AND m.proximoIntento <= :ahora "
            + "ORDER BY m.creado")
    List<MensajeSaliente> buscarListos(@Param("estado") EstadoMensaje estado, @Param("ahora") LocalDateTime ahora,
                                       Pageable pagina);

    long countByEstado(EstadoMensaje estado);

    List<MensajeSaliente> findByDestino(String destino);

    @Modifying
    @Query("DELETE FROM MensajeSaliente m WHERE m.estado = :estado AND m.enviado < :limite")
    int borrarEnviadosAntesDe(@Param("estado") EstadoMensaje estado, @Param("limite") LocalDateTime limite);
}
