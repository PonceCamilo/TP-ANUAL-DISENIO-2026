package ar.utn.donatrack.donaciones.exceptions.csvExcepctions;

public class CsvFormatoMailException extends RuntimeException {
  public CsvFormatoMailException(int numeroLinea) {
    super("Línea " + numeroLinea + ": el email es obligatorio");
  }
}
