package ar.utn.donatrack.donaciones.exceptions.comunes;

/**
 * La base tiene un nombre de estado que el código no reconoce.
 *
 * No es un error de quien llama sino un dato inconsistente (una fila editada a
 * mano, un estado renombrado sin migrar los datos): por eso no extiende ninguna
 * categoría y GlobalExceptionHandler lo responde como 500. Antes se reutilizaba
 * TipoDesconocidoException, que es un 400 pensado para datos de entrada.
 */
public class EstadoDesconocidoException extends RuntimeException {

    public EstadoDesconocidoException(String tipoDeEstado, String nombre) {
        super("Estado de " + tipoDeEstado + " desconocido en la base: '" + nombre + "'.");
    }
}
