package ar.utn.donatrack.donaciones.models.categoria;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Unidad mínima de asignación dentro del sistema.
 * Cada Donacion resultante de la segmentación queda asociada a una única Subcategoria.
 *
 * La igualdad se basa en el tipo normalizado (lowercase, sin espacios extra)
 * para evitar que "Arroz" y "arroz" sean subcategorías distintas.
 */

@Getter
@Setter
@EqualsAndHashCode(of = "tipoNormalizado")
public class Subcategoria {

    private final String tipo;
    private final String tipoNormalizado;

    public Subcategoria(String tipo) {
        this.tipo = tipo;
        this.tipoNormalizado = tipo == null ? "" : tipo.trim().toLowerCase();
    }

    @Override
    public String toString() {
        return tipo;
    }
}
