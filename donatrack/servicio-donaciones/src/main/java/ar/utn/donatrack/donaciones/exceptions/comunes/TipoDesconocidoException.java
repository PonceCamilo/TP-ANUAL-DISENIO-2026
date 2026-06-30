package ar.utn.donatrack.donaciones.exceptions.comunes;

/**
 * Excepción reutilizable para subtipos no reconocidos al resolver una jerarquía
 * polimórfica (ej: tipo de donante, de medio de contacto o de necesidad desconocido).
 * El tipo concreto se pasa como parámetro para no hardcodear el mensaje en cada uso.
 */
public class TipoDesconocidoException extends RuntimeException {
    public TipoDesconocidoException(String tipo) {
        super("Tipo de " + tipo + " desconocido.");
    }
}
