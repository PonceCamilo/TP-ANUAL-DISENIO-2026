package ar.utn.donatrack.autenticacion.repositories;

import ar.utn.donatrack.autenticacion.models.Activacion;
import ar.utn.donatrack.autenticacion.models.Cuenta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface Activaciones extends JpaRepository<Activacion, UUID> {
    Optional<Activacion> findByCodigoHash(String codigoHash);

    Optional<Activacion> findFirstByCuentaOrderByCreadaEnDesc(Cuenta cuenta);

    /** Pedir un enlace nuevo invalida los anteriores que no se usaron. */
    @Modifying
    @Query("delete from Activacion a where a.cuenta = ?1 and a.usadaEn is null")
    int borrarPendientes(Cuenta cuenta);
}
