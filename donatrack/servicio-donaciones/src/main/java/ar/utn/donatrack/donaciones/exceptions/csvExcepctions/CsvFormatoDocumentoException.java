package ar.utn.donatrack.donaciones.exceptions.csvExcepctions;

public class CsvFormatoDocumentoException extends RuntimeException {
  public CsvFormatoDocumentoException(int numeroLinea) {
    super("Línea " + numeroLinea + ": el documento no puede estar vacío");
  }
}
