package ar.utn.donatrack.donaciones.models.categoria;

import ar.utn.donatrack.donaciones.util.NormalizadorTexto;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Unidad mínima de asignación dentro del sistema.
 * Cada Donacion resultante de la segmentación queda asociada a una única Subcategoria.
 *
 * La igualdad se basa en el tipo normalizado (ver NormalizadorTexto: sin
 * acentos, mayúsculas ni espacios de más) para evitar que "Arroz" y "arroz"
 * sean subcategorías distintas. De eso depende la segmentación, que usa la
 * subcategoría como clave de un Map, y el matching contra las necesidades.
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

    /** El tipo se guarda sin espacios al principio ni al final, como se escribió. */
    public Subcategoria(String tipo) {
        this.tipo = tipo == null ? null : tipo.strip();
        this.tipoNormalizado = NormalizadorTexto.normalizar(tipo);
    }

    /**
     * Los tipos de las subcategorías sin repetir, comparadas en su forma
     * normalizada y en el orden en que aparecen: "Arroz", " arroz " y "ARROZ"
     * son una sola. Se informa el tipo como se escribió la primera vez.
     */
    public static List<String> tiposDistintos(Collection<Subcategoria> subcategorias) {
        return subcategorias.stream()
                .filter(Objects::nonNull)
                .filter(subcategoria -> !subcategoria.estaVacia())
                .distinct()
                .map(Subcategoria::getTipo)
                .toList();
    }

    public boolean estaVacia() {
        return tipoNormalizado == null || tipoNormalizado.isEmpty();
    }

    /** Misma subcategoría, normalizada. Una vacía no coincide con ninguna. */
    public boolean coincideCon(Subcategoria otra) {
        return otra != null && !estaVacia() && tipoNormalizado.equals(otra.tipoNormalizado);
    }

    /** Si el texto (un nombre, una descripción) menciona esta subcategoría. */
    public boolean estaMencionadaEn(String texto) {
        return !estaVacia() && NormalizadorTexto.normalizar(texto).contains(tipoNormalizado);
    }

    @Override
    public String toString() {
        return tipo;
    }
}
