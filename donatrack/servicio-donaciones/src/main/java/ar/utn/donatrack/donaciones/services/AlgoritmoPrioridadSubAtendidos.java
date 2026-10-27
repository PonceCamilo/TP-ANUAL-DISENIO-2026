package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.RecepcionesEntidadRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/**
 * Prioriza a las entidades que recibieron menos donaciones en el último
 * trimestre: puntaje = 1 / (1 + donaciones recibidas en los últimos 3 meses).
 *
 * DESNORMALIZACIÓN (Entrega 5): las recepciones salen del contador
 * entidad_recepcion_diaria y no de recorrer la tabla donacion. Antes, para
 * puntuar cada entidad se leían todas las donaciones con sus bienes e historial;
 * ahora prepararPuntaje trae los totales de todas las entidades en una sola
 * consulta agregada.
 */
@Component
@RequiredArgsConstructor
public class AlgoritmoPrioridadSubAtendidos extends AlgoritmoAsignacionBase {

    private static final int MESES_TRIMESTRE = 3;

    private final RecepcionesEntidadRepositoryInterface recepciones;

    @Override
    protected double calcularPuntaje(Donacion donacion, EntidadBeneficiaria entidad) {
        return puntaje(recepciones.contarRecepcionesDesde(entidad.getId(), inicioDelTrimestre()));
    }

    @Override
    protected ToDoubleFunction<EntidadBeneficiaria> prepararPuntaje(Donacion donacion,
                                                                  List<EntidadBeneficiaria> entidades) {
        Map<UUID, Long> recibidas = recepciones.contarRecepcionesDesde(inicioDelTrimestre());
        return entidad -> puntaje(recibidas.getOrDefault(entidad.getId(), 0L));
    }

    private static LocalDate inicioDelTrimestre() {
        return FechaHoraArgentina.hoy().minusMonths(MESES_TRIMESTRE);
    }

    private static double puntaje(long donacionesRecibidas) {
        return 1.0 / (1 + donacionesRecibidas);
    }
}
