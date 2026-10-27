package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.services.AlgoritmoAsignacion;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Clase base de los algoritmos de asignación (patrón Template Method sobre
 * el contrato AlgoritmoAsignacion).
 *
 * Centraliza el esqueleto común de TODO algoritmo —puntuar cada entidad,
 * descartar las que no apliquen, ordenar de mayor a menor puntaje y recortar
 * al top N— de modo que un algoritmo nuevo solo deba implementar cómo se
 * calcula el puntaje. Así se cumple el requisito del enunciado de poder
 * incorporar algoritmos nuevos sin tocar el orquestador ni duplicar lógica.
 */
public abstract class AlgoritmoAsignacionBase implements AlgoritmoAsignacion {

    protected static final int TOP_N = 10;

    @Override
    public final List<ResultadoAsignacion> evaluar(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        ToDoubleFunction<EntidadBeneficiaria> puntaje = prepararPuntaje(donacion, entidades);
        return entidades.stream()
                .map(entidad -> new ResultadoAsignacion(entidad.getId(), puntaje.applyAsDouble(entidad)))
                .filter(resultado -> incluir(resultado.getPuntaje()))
                .sorted(Comparator.comparingDouble(ResultadoAsignacion::getPuntaje).reversed())
                .limit(TOP_N)
                .toList();
    }

    /** Cada algoritmo define cómo puntúa la afinidad entre la donación y la entidad. */
    protected abstract double calcularPuntaje(Donacion donacion, EntidadBeneficiaria entidad);

    /**
     * Hook opcional (Entrega 5): permite que un algoritmo cargue de una sola vez
     * los datos que necesita para puntuar a todas las entidades, en lugar de
     * consultarlos entidad por entidad. Por defecto puntúa con calcularPuntaje.
     */
    protected ToDoubleFunction<EntidadBeneficiaria> prepararPuntaje(Donacion donacion,
                                                                  List<EntidadBeneficiaria> entidades) {
        return entidad -> calcularPuntaje(donacion, entidad);
    }

    /**
     * Hook opcional: decide si una entidad con cierto puntaje entra al ranking.
     * Por defecto entran todas; un algoritmo puede sobreescribirlo (ej: descartar
     * las que tienen puntaje 0 por no tener ninguna necesidad compatible).
     */
    protected boolean incluir(double puntaje) {
        return true;
    }
}
