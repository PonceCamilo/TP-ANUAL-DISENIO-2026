package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class CargaDonacion {
    private final UUID idDonante;
    private final String descripcion;
    private final List<Bien> bienes;

    public List<Donacion> segmentar() {
        List<Donacion> resultado = new ArrayList<>();

        Map<Subcategoria, List<Bien>> porSubcategoria = bienes.stream()
            .collect(Collectors.groupingBy(
                Bien::getSubcategoria,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        for (Map.Entry<Subcategoria, List<Bien>> entry : porSubcategoria.entrySet()) {
            Subcategoria sub = entry.getKey();
            List<Bien> bienesDeSubcat = entry.getValue();

            bienesDeSubcat.stream()
                .filter(BienPerecible.class::isInstance)
                .map(b -> (BienPerecible) b)
                .collect(Collectors.groupingBy(
                    BienPerecible::getFechaVencimiento,
                    LinkedHashMap::new,
                    Collectors.toList()
                ))
                .forEach((fecha, grupo) -> resultado.add(crearDonacion(sub, grupo)));

            bienesDeSubcat.stream()
                .filter(BienConEstado.class::isInstance)
                .map(b -> (BienConEstado) b)
                .collect(Collectors.groupingBy(
                    BienConEstado::isEsNuevo,
                    LinkedHashMap::new,
                    Collectors.toList()
                ))
                .forEach((esNuevo, grupo) -> resultado.add(crearDonacion(sub, grupo)));

            List<Bien> genericos = bienesDeSubcat.stream()
                .filter(b -> !(b instanceof BienPerecible) && !(b instanceof BienConEstado))
                .toList();

            if (!genericos.isEmpty()) {
                resultado.add(crearDonacion(sub, genericos));
            }
        }

        return resultado;
    }

    private Donacion crearDonacion(Subcategoria subcategoria, List<? extends Bien> grupo) {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(idDonante);
        donacion.setDescripcion(descripcion);
        donacion.setSubcategoria(subcategoria);
        donacion.getBienes().addAll(grupo);
        return donacion;
    }
}
