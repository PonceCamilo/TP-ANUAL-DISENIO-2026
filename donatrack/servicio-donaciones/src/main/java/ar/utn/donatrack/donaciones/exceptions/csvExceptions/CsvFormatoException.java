package ar.utn.donatrack.donaciones.exceptions.csvExceptions;

import ar.utn.donatrack.donaciones.exceptions.SolicitudInvalidaException;

/**
 * Una fila del CSV de importación no cumple el formato esperado.
 *
 * No corta la importación: CsvImportService la registra en el informe para esa
 * fila y sigue con la siguiente. Los distintos motivos son métodos de fábrica
 * y no subclases, porque nadie los distingue por tipo: solo cambia el mensaje.
 */
public class CsvFormatoException extends SolicitudInvalidaException {

    private CsvFormatoException(int numeroLinea, String detalle) {
        super("Línea " + numeroLinea + ": " + detalle);
    }

    public static CsvFormatoException columnasInsuficientes(int numeroLinea, int columnas) {
        return new CsvFormatoException(numeroLinea, "se esperaban al menos 5 columnas, se encontraron " + columnas);
    }

    public static CsvFormatoException tipoPersonaInvalido(int numeroLinea, String tipoPersona) {
        return new CsvFormatoException(numeroLinea,
                "tipo de persona inválido '" + tipoPersona + "'. Valores aceptados: HUMANA, JURIDICA");
    }

    public static CsvFormatoException emailVacio(int numeroLinea) {
        return new CsvFormatoException(numeroLinea, "el email es obligatorio");
    }

    public static CsvFormatoException documentoVacio(int numeroLinea) {
        return new CsvFormatoException(numeroLinea, "el documento no puede estar vacío");
    }

    public static CsvFormatoException nombreVacio(int numeroLinea) {
        return new CsvFormatoException(numeroLinea, "el nombre/razón social no puede estar vacío");
    }
}
