package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.util.Pagina;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Respuesta paginada de los listados (GET /donaciones y GET /donantes).
 *
 * Se usa un DTO propio y no el Page de Spring Data porque Spring advierte que el
 * JSON de PageImpl no es estable entre versiones. Así el contrato lo define el
 * servicio, y el mismo DTO sirve para cualquier listado.
 */
@Getter
@Builder
public class PaginaResponseDTO<T> {

    private final List<T> contenido;

    /** Número de la página devuelta (empieza en 0). */
    private final int pagina;

    /** Tamaño de página pedido. La última página puede traer menos elementos. */
    private final int tamanio;

    /** Cantidad total de elementos que cumplen el filtro, en todas las páginas. */
    private final long totalElementos;

    private final int totalPaginas;

    public static <T> PaginaResponseDTO<T> desde(Pagina<T> pagina) {
        return PaginaResponseDTO.<T>builder()
                .contenido(pagina.contenido())
                .pagina(pagina.numero())
                .tamanio(pagina.tamanio())
                .totalElementos(pagina.totalElementos())
                .totalPaginas(pagina.totalPaginas())
                .build();
    }
}
