package ar.utn.donatrack.donaciones.util;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados y el total de elementos que cumplen el filtro.
 *
 * Es el tipo que devuelven los puertos de repositorio (Entrega 5) en lugar del
 * Page de Spring Data: los services no dependen de la tecnología de
 * persistencia, igual que con el resto de los métodos de los repositorios.
 */
public record Pagina<T>(List<T> contenido, int numero, int tamanio, long totalElementos) {

    public int totalPaginas() {
        return tamanio == 0 ? 0 : (int) Math.ceil((double) totalElementos / tamanio);
    }

    public <R> Pagina<R> map(Function<T, R> transformacion) {
        return new Pagina<>(contenido.stream().map(transformacion).toList(), numero, tamanio, totalElementos);
    }
}
