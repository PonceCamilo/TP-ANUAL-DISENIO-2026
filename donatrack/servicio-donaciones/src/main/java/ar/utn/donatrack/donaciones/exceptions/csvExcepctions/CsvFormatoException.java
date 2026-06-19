package ar.utn.donatrack.donaciones.exceptions.csvExcepctions;

/**
 * Raíz de la jerarquía de excepciones de formato del CSV de importación.
 * Permite capturar en un único catch cualquier error de formato de fila
 * (línea, persona, mail, documento, nombre) sin enumerarlas una por una.
 */
public abstract class CsvFormatoException extends RuntimeException {

    public CsvFormatoException(String message) {
        super(message);
    }

    public CsvFormatoException(String message, Throwable cause) {
        super(message, cause);
    }
}
