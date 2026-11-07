package ar.utn.donatrack.donaciones.util;

import ar.utn.donatrack.donaciones.exceptions.comunes.PaginaInvalidaException;

/**
 * Qué página pide quien consulta un listado.
 *
 * POR QUÉ HAY PAGINACIÓN (Entrega 5): los listados devolvían la tabla completa
 * con todas sus colecciones. Después de importar el CSV de 20.000 donantes,
 * GET /donantes traía 20.000 personas con sus contactos en una sola respuesta.
 *
 * Las páginas empiezan en 0. Si no se indica tamaño se usan 20 elementos, y el
 * máximo es 100 para que nadie pueda volver a pedir la tabla entera. Un pedido
 * fuera de rango es un error de quien llama (400), no se corrige en silencio.
 */
public record SolicitudPagina(int numero, int tamanio) {

    public static final int TAMANIO_POR_DEFECTO = 20;
    public static final int TAMANIO_MAXIMO = 100;

    public SolicitudPagina {
        if (numero < 0) {
            throw new PaginaInvalidaException("El número de página no puede ser negativo (las páginas empiezan en 0).");
        }
        if (tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new PaginaInvalidaException(
                    "El tamaño de página debe estar entre 1 y " + TAMANIO_MAXIMO + ".");
        }
    }

    /** Arma la solicitud a partir de los parámetros opcionales de la URL. */
    public static SolicitudPagina de(Integer numero, Integer tamanio) {
        return new SolicitudPagina(
                numero == null ? 0 : numero,
                tamanio == null ? TAMANIO_POR_DEFECTO : tamanio);
    }
}
