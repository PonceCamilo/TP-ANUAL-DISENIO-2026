package ar.utn.donatrack.donaciones.interfaces.repositories;

import java.util.UUID;

/**
 * Criterios opcionales del listado de donaciones. Un campo nulo no filtra; los
 * textos en blanco se convierten en nulos al construir el filtro, así quien lo
 * usa solo pregunta por null. Los filtros se aplican en la base (Entrega 5):
 * antes se traían todas las donaciones y se filtraban en memoria.
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

    public FiltroDonaciones {
        estado = textoONulo(estado);
        subcategoria = textoONulo(subcategoria);
    }

    private static String textoONulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
