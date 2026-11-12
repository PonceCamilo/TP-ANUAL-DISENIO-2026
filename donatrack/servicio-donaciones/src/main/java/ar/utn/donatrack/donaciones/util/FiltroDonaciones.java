package ar.utn.donatrack.donaciones.util;

import ar.utn.donatrack.donaciones.exceptions.comunes.RangoDeFechasInvalidoException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Criterios opcionales del listado de donaciones. Un campo nulo no filtra; los
 * textos en blanco se convierten en nulos al construir el filtro, así quien lo
 * usa solo pregunta por null. Los filtros se aplican en la base (Entrega 5):
 * antes se traían todas las donaciones y se filtraban en memoria.
 *
 * @param estado        nombre del estado, por ejemplo EN_DEPOSITO
 * @param idDonante     donaciones de una persona donante ("mis donaciones")
 * @param subcategorias una o más subcategorías, sin distinguir mayúsculas: trae
 *                      las de cualquiera de ellas (Entrega 6, requisito 5). Vacía
 *                      no filtra; los textos en blanco y los repetidos se descartan.
 * @param idEntidad     donaciones asignadas a una entidad beneficiaria
 * @param soloConBienesVencidos donaciones con algún perecedero ya vencido (las
 *                      que la persona administradora tiene que pasar a VENCIDA)
 * @param desde         donaciones hechas ese día o después
 * @param hasta         donaciones hechas ese día o antes; los dos extremos se incluyen
 */
public record FiltroDonaciones(String estado, UUID idDonante, List<String> subcategorias, UUID idEntidad,
                               boolean soloConBienesVencidos, LocalDate desde, LocalDate hasta) {

    public FiltroDonaciones {
        estado = textoONulo(estado);
        subcategorias = subcategorias == null ? List.of() : subcategorias.stream()
                .map(FiltroDonaciones::textoONulo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new RangoDeFechasInvalidoException(
                    "La fecha 'desde' (" + desde + ") no puede ser posterior a 'hasta' (" + hasta + ")");
        }
    }

    /** Con una sola subcategoría (o ninguna) y sin rango de fechas. */
    public FiltroDonaciones(String estado, UUID idDonante, String subcategoria, UUID idEntidad,
                            boolean soloConBienesVencidos) {
        this(estado, idDonante, subcategoria == null ? List.of() : List.of(subcategoria), idEntidad,
                soloConBienesVencidos, null, null);
    }

    /** El mismo filtro, limitado a las donaciones de una persona donante (Entrega 6: ControlDeAcceso). */
    public FiltroDonaciones conDonante(UUID id) {
        return new FiltroDonaciones(estado, id, subcategorias, idEntidad, soloConBienesVencidos, desde, hasta);
    }

    /** El mismo filtro, limitado a las donaciones asignadas a una entidad (Entrega 6: ControlDeAcceso). */
    public FiltroDonaciones conEntidad(UUID id) {
        return new FiltroDonaciones(estado, idDonante, subcategorias, id, soloConBienesVencidos, desde, hasta);
    }

    private static String textoONulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
