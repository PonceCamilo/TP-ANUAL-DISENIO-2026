package ar.utn.donatrack.donaciones.models.categoria;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Unidad mínima de asignación dentro del sistema.
 * Cada Donacion resultante de la segmentación queda asociada a una única Subcategoria.
 *
 * La igualdad se basa en el tipo normalizado (lowercase, sin espacios extra)
 * para evitar que "Arroz" y "arroz" sean subcategorías distintas. De eso depende
 * la segmentación, que usa la subcategoría como clave de un Map.
 *
 * MAPEO (Entrega 4): es un @Embeddable, así que sus dos columnas viven en la
 * tabla de quien la contiene (donacion y bien) en lugar de tener tabla propia.
 * Es lo que corresponde a un objeto de valor: no tiene identidad ni ciclo de
 * vida independiente.
 *
 * Los campos dejaron de ser `final` porque JPA necesita construir la instancia
 * con el constructor sin argumentos y después inyectar los valores. Se mantiene
 * la ausencia de setters públicos, así que sigue siendo inmutable de hecho.
 */
@Embeddable
@Getter
@EqualsAndHashCode(of = "tipoNormalizado")
public class Subcategoria {

    @Column(name = "subcategoria_tipo")
    private String tipo;

    @Column(name = "subcategoria_tipo_normalizado")
    private String tipoNormalizado;

    /** Requerido por JPA. No usar desde el dominio. */
    protected Subcategoria() {
    }

    public Subcategoria(String tipo) {
        this.tipo = tipo;
        this.tipoNormalizado = tipo == null ? "" : tipo.trim().toLowerCase();
    }

    @Override
    public String toString() {
        return tipo;
    }
}
