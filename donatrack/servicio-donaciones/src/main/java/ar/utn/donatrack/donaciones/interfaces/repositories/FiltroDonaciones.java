package ar.utn.donatrack.donaciones.interfaces.repositories;

import java.util.UUID;

/**
 * Criterios opcionales del listado de donaciones. Un campo nulo o en blanco no
 * filtra. Los filtros se aplican en la base (Entrega 5): antes se traían todas
 * las donaciones y se filtraban en memoria.
 *
 * @param estado       nombre del estado, por ejemplo EN_DEPOSITO
 * @param idDonante    donaciones de una persona donante ("mis donaciones")
 * @param subcategoria subcategoría, sin distinguir mayúsculas
 * @param idEntidad    donaciones asignadas a una entidad beneficiaria
 * @param soloConBienesVencidos donaciones con algún perecedero ya vencido (las
 *                     que la persona administradora tiene que pasar a VENCIDA)
 */
public record FiltroDonaciones(String estado, UUID idDonante, String subcategoria, UUID idEntidad,
                               boolean soloConBienesVencidos) {

    public FiltroDonaciones(String estado, UUID idDonante, String subcategoria, UUID idEntidad) {
        this(estado, idDonante, subcategoria, idEntidad, false);
    }

    public static FiltroDonaciones sinFiltros() {
        return new FiltroDonaciones(null, null, null, null);
    }
}
