package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.AlgoritmoAsignacionInterface;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * prioriza entidades que recibieron menos donaciones en el último trimestre
 */
@Component
@RequiredArgsConstructor
public class AlgoritmoPrioridadSubAtendidos implements AlgoritmoAsignacionInterface {

    private static final int TOP_N = 10;
    private static final int MESES_TRIMESTRE = 3;

    private final DonacionesRepositoryInterface donacionesRepository;

    @Override
    public List<ResultadoAsignacion> evaluar(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        LocalDate fechaLimite = LocalDate.now().minusMonths(MESES_TRIMESTRE);
        List<Donacion> todasLasDonaciones = donacionesRepository.obtenerTodas();

        return entidades.stream()
                .map(entidad -> new ResultadoAsignacion(
                        entidad.getId(),
                        calcularPuntaje(entidad, todasLasDonaciones, fechaLimite)))
                .sorted(Comparator.comparingDouble(ResultadoAsignacion::getPuntaje).reversed())
                .limit(TOP_N)
                .toList();
    }

    private double calcularPuntaje(EntidadBeneficiaria entidad, List<Donacion> todasLasDonaciones, LocalDate fechaLimite) {
        long donacionesRecibidas = todasLasDonaciones.stream()
                .filter(d -> entidad.getId().equals(d.getIdEntidadAsignada()))
                .filter(d -> d.getFechaAsignacion() != null && !d.getFechaAsignacion().isBefore(fechaLimite))
                .count();

        // a menor cantidad de donaciones recibidas, mayor puntaje (más prioridad)
        return 1.0 / (1 + donacionesRecibidas);
    }
}
