package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Prioriza a las entidades que menos donaciones recibieron en el último
 * trimestre: a menor cantidad recibida, mayor puntaje (más prioridad).
 * Esto reparte las donaciones y evita concentrarlas en pocas entidades.
 */
@Component
@RequiredArgsConstructor
public class AlgoritmoPrioridadSubAtendidos extends AlgoritmoAsignacionBase {

    private static final int MESES_TRIMESTRE = 3;

    private final DonacionesRepositoryInterface donacionesRepository;

    @Override
    protected double calcularPuntaje(Donacion donacion, EntidadBeneficiaria entidad) {
        LocalDate fechaLimite = LocalDate.now().minusMonths(MESES_TRIMESTRE);

        long donacionesRecibidas = donacionesRepository.obtenerTodas().stream()
                .filter(d -> entidad.getId().equals(d.getIdEntidadBeneficiaria()))
                .filter(d -> d.getFechaAsignacion() != null && !d.getFechaAsignacion().isBefore(fechaLimite))
                .count();

        return 1.0 / (1 + donacionesRecibidas);
    }
}
