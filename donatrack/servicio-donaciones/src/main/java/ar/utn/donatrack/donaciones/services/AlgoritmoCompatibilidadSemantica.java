package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.services.AlgoritmoAsignacionInterface;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Evalúa la correspondencia entre la subcategoría del bien donado
 * y las necesidades declaradas por cada entidad beneficiaria.
 * Cuantas más necesidades coincidentes tenga la entidad, mayor su puntaje.
 */
@Component
public class AlgoritmoCompatibilidadSemantica implements AlgoritmoAsignacionInterface {

    private static final int TOP_N = 10;

    @Override
    public List<ResultadoAsignacion> evaluar(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        return entidades.stream()
                .map(entidad -> new ResultadoAsignacion(
                        entidad.getId(),
                        contarCoincidencias(donacion, entidad))) /** para cada entidad creo un result con id y puntaje, voy a tener esto por cada entidad*/
                .filter(resultado -> resultado.getPuntaje() > 0) /** descarto las que tengo 0*/
                .sorted(Comparator.comparingDouble(ResultadoAsignacion::getPuntaje).reversed())
                .limit(TOP_N)
                .toList();
    }

    private double contarCoincidencias(Donacion donacion, EntidadBeneficiaria entidad) {
        return entidad.getCampanias().stream()
                .flatMap(c -> c.getNecesidades().stream()) /** pongo todas las necesidades en de todas las campanias en una sola lista*/
                .filter(n -> coincideSubcategoria(n, donacion))
                .count();
    }

    private boolean coincideSubcategoria(Necesidad necesidad, Donacion donacion) {
        return necesidad.getBien() != null
                && necesidad.getBien().getSubcategoria() != null
                && necesidad.getBien().getSubcategoria().equals(donacion.getSubcategoria());
    }
}