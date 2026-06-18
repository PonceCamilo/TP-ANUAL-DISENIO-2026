package ar.utn.donatrack.donaciones.exceptions.csvExcepctions;

/**
 * Excepción lanzada cuando una fila del CSV no cumple con el formato esperado.
 * Es una excepción de negocio: no detiene el proceso de importación,
 * sino que se registra en el ImportReport y se continúa con la siguiente fila.
 */

public class CsvFormatoLineaException extends CsvFormatoException {

    public CsvFormatoLineaException(int numeroLinea, int columnas) {
        super("Línea " + numeroLinea + ": se esperaban al menos 5 columnas, "
              + "se encontraron " + columnas);
    }
}
