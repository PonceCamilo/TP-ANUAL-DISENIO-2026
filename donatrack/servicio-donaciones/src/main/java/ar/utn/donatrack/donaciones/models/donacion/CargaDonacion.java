package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Todo lo que una persona donante entrega en una sola carga, antes de segmentarlo.
 *
 * El enunciado pide que la carga se separe automáticamente en donaciones de una
 * única subcategoría, y que dentro de una subcategoría se separen los
 * perecederos con vencimientos distintos y los bienes nuevos de los usados.
 */
@Getter
@AllArgsConstructor
public class CargaDonacion {
    private final UUID idDonante;
    private final String descripcion;
    private final List<Bien> bienes;

    /**
     * Una donación por cada clave de segmentación distinta, en el orden en que
     * aparecen en la carga. La clave la arma cada bien (Bien.claveDeSegmentacion):
     * acá no se pregunta qué tipo de bien es cada uno.
     */
    public List<Donacion> segmentar() {
        return bienes.stream()
                .collect(Collectors.groupingBy(Bien::claveDeSegmentacion, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(grupo -> crearDonacion(grupo.getFirst().getSubcategoria(), grupo))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Donacion crearDonacion(Subcategoria subcategoria, List<Bien> grupo) {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(idDonante);
        donacion.setDescripcion(descripcion);
        donacion.setSubcategoria(subcategoria);
        donacion.getBienes().addAll(grupo);
        return donacion;
    }
}
