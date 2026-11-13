package ar.utn.donatrack.autenticacion.repositories;

import ar.utn.donatrack.autenticacion.models.Cuenta;
import ar.utn.donatrack.autenticacion.models.CuentaDonante;
import ar.utn.donatrack.autenticacion.models.CuentaEntidadBeneficiaria;
import ar.utn.donatrack.autenticacion.models.Rol;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface Cuentas extends JpaRepository<Cuenta, UUID> {
    Optional<Cuenta> findByEmailAndRol(String email, Rol rol);
    List<Cuenta> findByRolOrderByNombreAsc(Rol rol);

    @Query("select d from CuentaDonante d where d.googleSub = ?1")
    Optional<CuentaDonante> buscarDonantePorGoogleSub(String googleSub);

    @Query("select count(d) > 0 from CuentaDonante d where d.donanteId = ?1")
    boolean existeDonanteVinculadoA(UUID donanteId);

    @Query("select e from CuentaEntidadBeneficiaria e where e.googleSub = ?1")
    Optional<CuentaEntidadBeneficiaria> buscarEntidadPorGoogleSub(String googleSub);
}
