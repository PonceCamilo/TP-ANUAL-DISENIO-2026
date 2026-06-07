package ar.utn.donatrack.donaciones.exceptions;

/**
 * Excepción lanzada cuando una fila del CSV no cumple con el formato esperado.
 * Es una excepción de negocio: no detiene el proceso de importación,
 * sino que se registra en el ImportReport y se continúa con la siguiente fila.
 */
public class CsvFormatoInvalidoException extends RuntimeException {

    public CsvFormatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
