package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import org.springframework.stereotype.Component;

/**
 * Evalúa la correspondencia entre la subcategoría del bien donado y las
 * necesidades declaradas por cada entidad beneficiaria.
 *
 * El puntaje es la cantidad de necesidades de la entidad cuyo nombre o
 * descripción mencionan la subcategoría de la donación. Cuantas más
 * necesidades coincidentes, mayor el puntaje. Las entidades sin ninguna
 * coincidencia (puntaje 0) quedan fuera del ranking.
 */
@Component
public class AlgoritmoCompatibilidadSemantica extends AlgoritmoAsignacionBase {

    @Override
    protected double calcularPuntaje(Donacion donacion, EntidadBeneficiaria entidad) {
        String subcategoria = donacion.getSubcategoria() != null ? donacion.getSubcategoria().getTipo() : null;
        if (subcategoria == null || subcategoria.isBlank() || entidad.getCampanias() == null) {
            return 0;
        }
        String objetivo = subcategoria.toLowerCase();
        return entidad.getCampanias().stream()
                .filter(campania -> campania.getNecesidades() != null)
                .flatMap(campania -> campania.getNecesidades().stream())
                .filter(necesidad -> contieneTexto(necesidad.getNombre(), objetivo)
                        || contieneTexto(necesidad.getDescripcion(), objetivo))
                .count();
    }

    /** Solo entran al ranking las entidades con al menos una necesidad compatible. */
    @Override
    protected boolean incluir(double puntaje) {
        return puntaje > 0;
    }

    private boolean contieneTexto(String texto, String objetivo) {
        return texto != null && texto.toLowerCase().contains(objetivo);
    }
}
