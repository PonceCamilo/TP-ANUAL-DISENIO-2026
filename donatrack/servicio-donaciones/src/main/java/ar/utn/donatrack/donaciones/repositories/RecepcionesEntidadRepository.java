package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioRecepcionesEntidad;
import ar.utn.donatrack.donaciones.repositories.jpa.RecepcionDiariaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RecepcionesEntidadRepository implements RepositorioRecepcionesEntidad {

    private final RecepcionDiariaJpaRepository jpa;

    public Map<UUID, Long> contarRecepcionesDesde(LocalDate desde) {
        return jpa.totalesDesde(desde).stream()
                .collect(Collectors.toMap(
                        RecepcionDiariaJpaRepository.TotalPorEntidad::getIdEntidad,
                        RecepcionDiariaJpaRepository.TotalPorEntidad::getTotal));
    }

    public long contarRecepcionesDesde(UUID idEntidad, LocalDate desde) {
        return jpa.totalDesde(idEntidad, desde);
    }

    /**
     * Vaciar y recalcular tienen que ir en la misma transacción (la abre
     * ReconciliacionRecepcionesService): así nadie ve el contador a medias.
     */
    public int reconciliar() {
        jpa.vaciar();
        return jpa.recalcularDesdeDonaciones();
    }
}
