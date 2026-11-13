package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioRecepcionesEntidad;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
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
@Order(2)
@RequiredArgsConstructor
public class AlgoritmoPrioridadSubAtendidos extends AlgoritmoAsignacionBase {

    public static final String NOMBRE = "PRIORIDAD_SUBATENDIDOS";

    private static final int MESES_TRIMESTRE = 3;

    private final RepositorioRecepcionesEntidad recepciones;

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    protected double calcularPuntaje(Donacion donacion, EntidadBeneficiaria entidad) {
        if (!necesitaLoQueSeDona(donacion, entidad)) {
            return 0;
        }
        return puntaje(recepciones.contarRecepcionesDesde(entidad.getId(), inicioDelTrimestre()));
    }

    /**
     * Entrega 6: prioriza a las que menos recibieron, pero solo entre las que
     * piden lo que se dona. Antes puntuaba a todas las entidades, y la
     * administración terminaba asignando donaciones a quien no las necesitaba.
     */
    private static boolean necesitaLoQueSeDona(Donacion donacion, EntidadBeneficiaria entidad) {
        return donacion.getSubcategoria() != null && !donacion.getSubcategoria().estaVacia()
                && entidad.contarNecesidadesCompatiblesCon(donacion.getSubcategoria(), FechaHoraArgentina.hoy()) > 0;
    }

    @Override
    protected boolean incluir(double puntaje) {
        return puntaje > 0;
    }

    @Override
    protected ToDoubleFunction<EntidadBeneficiaria> prepararPuntaje(Donacion donacion,
                                                                  List<EntidadBeneficiaria> entidades) {
        Map<UUID, Long> recibidas = recepciones.contarRecepcionesPorEntidadDesde(inicioDelTrimestre());
        return entidad -> necesitaLoQueSeDona(donacion, entidad)
                ? puntaje(recibidas.getOrDefault(entidad.getId(), 0L)) : 0;
    }

    private static LocalDate inicioDelTrimestre() {
        return FechaHoraArgentina.hoy().minusMonths(MESES_TRIMESTRE);
    }

    private static double puntaje(long donacionesRecibidas) {
        return 1.0 / (1 + donacionesRecibidas);
    }
}
