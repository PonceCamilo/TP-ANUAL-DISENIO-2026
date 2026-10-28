package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import org.springframework.stereotype.Component;

/**
 * Evalúa la correspondencia entre la subcategoría del bien donado y las
 * necesidades declaradas por cada entidad beneficiaria.
 *
 * El puntaje es la cantidad de necesidades abiertas (sin satisfacer, en
 * campañas vigentes) compatibles con la subcategoría de la donación: la que la
 * necesidad declara o, si no declara ninguna, la mencionada en su nombre o
 * descripción. Cuantas más necesidades coincidentes, mayor el puntaje. Las
 * entidades sin ninguna coincidencia (puntaje 0) quedan fuera del ranking.
 */
@Component
public class AlgoritmoCompatibilidadSemantica extends AlgoritmoAsignacionBase {

    @Override
    protected double calcularPuntaje(Donacion donacion, EntidadBeneficiaria entidad) {
        if (donacion.getSubcategoria() == null || donacion.getSubcategoria().estaVacia()) {
            return 0;
        }
        return entidad.contarNecesidadesCompatiblesCon(donacion.getSubcategoria(), FechaHoraArgentina.hoy());
    }

    /** Solo entran al ranking las entidades con al menos una necesidad compatible. */
    @Override
    protected boolean incluir(double puntaje) {
        return puntaje > 0;
    }
}
