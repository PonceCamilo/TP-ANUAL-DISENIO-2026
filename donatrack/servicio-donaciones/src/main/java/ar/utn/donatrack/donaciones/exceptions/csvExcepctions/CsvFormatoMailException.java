package ar.utn.donatrack.donaciones.exceptions.csvExcepctions;

public class CsvFormatoMailException extends CsvFormatoException {
  public CsvFormatoMailException(int numeroLinea) {
    super("Línea " + numeroLinea + ": el email es obligatorio");
  }
}
