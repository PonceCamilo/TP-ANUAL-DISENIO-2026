package ar.utn.donatrack.donaciones.exceptions.csvExcepctions;

public class CsvFormatoNombreException extends RuntimeException {
  public CsvFormatoNombreException(int numeroLinea) {
    super("Línea " + numeroLinea + ": el nombre/razón social no puede estar vacío");
  }
}
