package ar.utn.donatrack.donaciones.util;

import ar.utn.donatrack.donaciones.exceptions.comunes.PaginaInvalidaException;

/**
 * Qué página pide quien consulta un listado.
 *
 * POR QUÉ HAY PAGINACIÓN (Entrega 5): los listados devolvían la tabla completa
 * con todas sus colecciones. Después de importar el CSV de 20.000 donantes,
 * GET /donantes traía 20.000 personas con sus contactos en una sola respuesta.
 *
 * Si no se indica tamaño se usan 20 elementos, y el máximo es 100 para que nadie
 * pueda volver a pedir la tabla entera. Un pedido fuera de rango es un error de
 * quien llama (400), no se corrige en silencio.
 *
 * NUMERACIÓN (Entrega 6): en la API las páginas empiezan en 1, que es como las
 * muestra la interfaz ("página 1 de 5"). Por dentro se sigue usando el índice
 * desde 0 de Spring Data: `numero` es ese índice. La conversión se hace en un
 * solo lugar al entrar (de) y en uno solo al salir (PaginaResponseDTO.desde), así
 * los repositorios y los services no cambian.
 *
 * @param numero  índice de la página, desde 0 (la página 1 de la API es el 0)
 * @param tamanio elementos por página
 */
public record SolicitudPagina(int numero, int tamanio) {

    public static final int PRIMERA_PAGINA = 1;
    public static final int TAMANIO_POR_DEFECTO = 20;
    public static final int TAMANIO_MAXIMO = 100;

    public SolicitudPagina {
        if (numero < 0) {
            throw new PaginaInvalidaException("El índice de página no puede ser negativo.");
        }
        if (tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new PaginaInvalidaException(
                    "El tamaño de página debe estar entre 1 y " + TAMANIO_MAXIMO + ".");
        }
    }

    /**
     * Arma la solicitud a partir de los parámetros opcionales de la URL.
     *
     * @param pagina  número de página tal como lo pide la API (desde 1); null es la primera
     * @param tamanio elementos por página; null es el tamaño por defecto
     */
    public static SolicitudPagina de(Integer pagina, Integer tamanio) {
        int numeroDePagina = pagina == null ? PRIMERA_PAGINA : pagina;
        if (numeroDePagina < PRIMERA_PAGINA) {
            throw new PaginaInvalidaException(
                    "El número de página debe ser " + PRIMERA_PAGINA + " o mayor (las páginas empiezan en 1).");
        }
        return new SolicitudPagina(
                numeroDePagina - PRIMERA_PAGINA,
                tamanio == null ? TAMANIO_POR_DEFECTO : tamanio);
    }
}
