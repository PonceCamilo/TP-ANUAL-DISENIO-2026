package ar.utn.donatrack.donaciones.exceptions.csvExcepctions;

public class CsvFormatoPersonaException extends RuntimeException {
  public CsvFormatoPersonaException(int numeroLinea, String tipoPersona) {
    super("Línea " + numeroLinea + ": tipo de persona inválido '" + tipoPersona
          + "'. Valores aceptados: HUMANA, JURIDICA");
  }
}
